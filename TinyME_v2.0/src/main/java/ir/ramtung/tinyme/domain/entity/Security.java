package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.entity.stats.AuctionStats;
import ir.ramtung.tinyme.domain.entity.stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.stats.SecurityStats;
import ir.ramtung.tinyme.domain.entity.stats.SituationalStats;
import ir.ramtung.tinyme.domain.entity.stats.StateStats;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.UnknownSecurityStateException;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.controls.AuctionMatchingControl;
import ir.ramtung.tinyme.domain.service.controls.ContinuousMatchingControl;
import ir.ramtung.tinyme.domain.service.controls.ControlResult;
import ir.ramtung.tinyme.domain.service.controls.CreditControl;
import ir.ramtung.tinyme.domain.service.controls.PositionControl;
import ir.ramtung.tinyme.domain.service.controls.QuantityControl;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Security {

	private String isin;

	@Builder.Default
	private int tickSize = 1;

	@Builder.Default
	private int lotSize = 1;

	@Builder.Default
	private OrderBook orderBook = new OrderBook();

	private int lastTradePrice;

	//FIXME: this is turning to something really ugly
	private static PositionControl positionControl = new PositionControl();
	private static CreditControl creditControl = new CreditControl();
	private static QuantityControl quantityControl = new QuantityControl();
	private static Matcher matcher = new Matcher(new ContinuousMatchingControl(positionControl, creditControl, quantityControl), new AuctionMatchingControl(positionControl, creditControl, quantityControl));

	@Builder.Default
	private SecurityState state = SecurityState.CONTINUOUS;

	public SecurityResponse addNewOrder(Order newOrder) {
		if (positionControl.checkPositionForOrder(newOrder, orderBook) != ControlResult.OK) {
			return new SecurityResponse(SituationalStats.createNotEnoughPositionsStats(newOrder.getOrderId()));
		}
		List<SecurityStats> stats = handleAdd(newOrder);
		return new SecurityResponse(stats);
	}

	private List<SecurityStats> handleAdd(Order newOrder) {
		if (this.state == SecurityState.CONTINUOUS) {
			return handleAddInContinuousState(newOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return handleAddInAuctionState(newOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> handleAddInAuctionState(Order newOrder) {
		if (creditControl.checkCreditForBeingQueued(newOrder) != ControlResult.OK) {
			return List.of(SituationalStats.createNotEnoughCreditStats(newOrder.getOrderId()));
		}

		creditControl.updateCreditForBeingQueued(newOrder);
		orderBook.enqueue(newOrder);

		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createAddOrderStats(newOrder.getOrderId()));
		stats.add(createAuctionStats());
		return stats;
	}

	private List<SecurityStats> handleAddInContinuousState(Order newOrder) {
		List<SecurityStats> stats = new ArrayList<>();
		if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
			stats.addAll(addNewStopLimitOrder(newStopLimitOrder));
		} else {
			stats.addAll(addNewLimitOrderInContinuousState(newOrder));
		}
		stats.addAll(activateStopLimitOrders());
		return stats;
	}

	private void updateLastTradePrice(List<Trade> trades) {
		if (!trades.isEmpty()) {
			lastTradePrice = trades.getLast().getPrice();
		}
	}

	private List<SecurityStats> addNewStopLimitOrder(StopLimitOrder newOrder) {
		if (creditControl.checkCreditForBeingQueued(newOrder) != ControlResult.OK) {
			return List.of(SituationalStats.createNotEnoughCreditStats(newOrder.getOrderId()));
		}

		creditControl.updateCreditForBeingQueued(newOrder);
		orderBook.enqueue(newOrder);
		return List.of(SituationalStats.createAddOrderStats(newOrder.getOrderId()));
	}

	private List<SecurityStats> addNewLimitOrderInContinuousState(Order newOrder) {
		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createAddOrderStats(newOrder.getOrderId()));

		MatchResult newOrderMatchResult = matcher.continuousExecuting(newOrder, orderBook);
		if (!newOrderMatchResult.isSuccessful()) {
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(newOrderMatchResult, newOrder.getOrderId()));
		}
		if(!newOrderMatchResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuousExecuteStats(newOrderMatchResult.trades(), newOrder.getOrderId()));
		}
		updateLastTradePrice(newOrderMatchResult.trades());
		return stats;
	}

	public SecurityResponse deleteOrder(Side side, long orderId) {
		Order order = findByOrderId(side, orderId);
		creditControl.updateCreditAtDelete(order);
		orderBook.removeOrder(order);

		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createDeleteOrderStats(orderId));
		if (this.state == SecurityState.AUCTION) {
			stats.add(createAuctionStats());
		}
		return new SecurityResponse(stats);
	}

	public SecurityResponse changeMatchingState(SecurityState newState) {
		StateStats stateStats = StateStats.createStateStats(this.state, newState);
		SecurityState prevState = this.state;
		this.state = newState;
		if (prevState == SecurityState.CONTINUOUS) {
			return new SecurityResponse(stateStats);
		} else if (prevState == SecurityState.AUCTION) {
			List<SecurityStats> stats = openAuction();
			stats.add(stateStats);
			stats.addAll(activateStopLimitOrders());
			return new SecurityResponse(stats);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> openAuction() {
		List<SecurityStats> stats = new ArrayList<>();

		List<Trade> trades = matcher.auctionExecuting(orderBook, lastTradePrice).trades();
		if (!trades.isEmpty()) {
			stats.add(ExecuteStats.createAuctionExecuteStats(trades));
		}

		updateLastTradePrice(trades);

		return stats;
	}

	public SecurityResponse updateOrder(Order tempOrder) {
		Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
		List<SecurityStats> stats = handleUpdate(tempOrder, mainOrder);
		return new SecurityResponse(stats);
	}

	private List<SecurityStats> handleUpdate(Order tempOrder, Order mainOrder) {
		boolean losesPriority = mainOrder.willPriorityLostInUpdate(tempOrder);
		if (losesPriority) {
			Order originalOrder = mainOrder.snapshot();
			creditControl.updateCreditAtDelete(mainOrder);
			orderBook.removeOrder(mainOrder);
			mainOrder.updateFromTempOrder(tempOrder);
			return reAddUpdatedOrder(mainOrder, originalOrder);
		} else {
			return updateByKeepingPriority(tempOrder, mainOrder);
		}
	}

	private List<SecurityStats> updateByKeepingPriority(Order tempOrder, Order mainOrder) {
		if (this.state == SecurityState.CONTINUOUS) {
			return updateByKeepingPriorityInContinuousState(tempOrder, mainOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return updateByKeepingPriorityInAuctionState(tempOrder, mainOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> updateByKeepingPriorityInContinuousState(Order tempOrder, Order mainOrder) {
		mainOrder.updateFromTempOrder(tempOrder);
		return List.of(SituationalStats.createUpdateOrderStats(mainOrder.getOrderId()));
	}

	private List<SecurityStats> updateByKeepingPriorityInAuctionState(Order tempOrder, Order mainOrder) {
		mainOrder.updateFromTempOrder(tempOrder);
		
		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createUpdateOrderStats(mainOrder.getOrderId()));
		stats.add(createAuctionStats());
		return stats;
	}

	private List<SecurityStats> reAddUpdatedOrder(Order updatedOrder, Order originalOrder) {
		if (positionControl.checkPositionForOrder(updatedOrder, orderBook) != ControlResult.OK) {
			creditControl.updateCreditForBeingQueued(originalOrder);
			orderBook.enqueue(originalOrder);
			return List.of(SituationalStats.createNotEnoughPositionsStats(originalOrder.getOrderId()));
		}
		
		if (this.state == SecurityState.CONTINUOUS) {
			return reAddUpdatedOrderInContinuousState(updatedOrder, originalOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return reAddUpdatedOrderInAuctionState(updatedOrder, originalOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> reAddUpdatedOrderInContinuousState(Order updatedOrder, Order originalOrder) {
		if (updatedOrder instanceof StopLimitOrder updatedSlo) {
			StopLimitOrder originalSlo = (StopLimitOrder) originalOrder;
			return reAddUpdatedSloInContinuousState(updatedSlo, originalSlo);
		} else {
			return reAddActiveOrderInContinuousState(updatedOrder, originalOrder);
		}
	}

	private List<SecurityStats> reAddUpdatedOrderInAuctionState(Order updatedOrder, Order originalOrder) {
		if (creditControl.checkCreditForBeingQueued(updatedOrder) != ControlResult.OK) {
			creditControl.updateCreditForBeingQueued(originalOrder);
			orderBook.enqueue(originalOrder);
			return List.of(SituationalStats.createNotEnoughCreditStats(originalOrder.getOrderId()));
		}

		creditControl.updateCreditForBeingQueued(updatedOrder);
		orderBook.enqueue(updatedOrder);

		List<SecurityStats> stats = new LinkedList<>();
		stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
		stats.add(createAuctionStats());
		return stats;
	}

	private List<SecurityStats> reAddActiveOrderInContinuousState(Order updatedOrder, Order originalOrder) {
		List<SecurityStats> stats = new LinkedList<>();
		stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
		
		MatchResult updatedOrderResult = matcher.continuousExecuting(updatedOrder, orderBook);

		if (!updatedOrderResult.isSuccessful()) {
			creditControl.updateCreditForBeingQueued(originalOrder);
			orderBook.enqueue(originalOrder);
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(updatedOrderResult, originalOrder.getOrderId()));
		} 
		if (!updatedOrderResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuousExecuteStats(updatedOrderResult.trades(), originalOrder.getOrderId()));
		}

		updateLastTradePrice(updatedOrderResult.trades());
		stats.addAll(activateStopLimitOrders());
		return stats;
	}

	private List<SecurityStats> reAddUpdatedSloInContinuousState(StopLimitOrder updatedOrder,StopLimitOrder originalOrder) {
			if (creditControl.checkCreditForBeingQueued(updatedOrder) != ControlResult.OK) {
				creditControl.updateCreditForBeingQueued(originalOrder);
				orderBook.enqueue(originalOrder);
				return List.of(SituationalStats.createNotEnoughCreditStats(originalOrder.getOrderId()));
			}

			creditControl.updateCreditForBeingQueued(updatedOrder);
			orderBook.enqueue(updatedOrder);

			List<SecurityStats> stats = new LinkedList<>();
			stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
			stats.addAll(activateStopLimitOrders());
			return stats;
	}

	public List<String> checkEnterOrderRq(EnterOrderRq order) {
		List<String> errors = new LinkedList<>();
		
		if (order.getQuantity() % lotSize != 0) {
			errors.add(Message.QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE);
		}

		if (order.getPrice() % tickSize != 0) {
			errors.add(Message.PRICE_NOT_MULTIPLE_OF_TICK_SIZE);
		}

		if(this.state == SecurityState.AUCTION && order.getMinimumExecutionQuantity() != 0) {
			errors.add(Message.MINIMUM_EXECUTION_IN_AUCTION_STATE);
		}

		if(this.state == SecurityState.AUCTION && order.getStopPrice() != 0) {
			errors.add(Message.STOP_PRICE_IN_AUCTION_STATE);
		}

		return errors;
	}

	private List<SecurityStats> activateStopLimitOrders() {
		List<SecurityStats> stats = new LinkedList<>();
		StopLimitOrder slo;

		while ((slo = orderBook.getStopLimitOrder(lastTradePrice)) != null) {
			creditControl.updateCreditAtDelete(slo);
			stats.add(SituationalStats.createOrderActivatedStats(slo.getOrderId(), slo.getRequestId()));
			Order activatedOrder = new Order(slo);
			if (this.state == SecurityState.CONTINUOUS) {
				stats.addAll(activateOrderInContinuousState(activatedOrder, slo.getRequestId()));
			} else if (this.state == SecurityState.AUCTION) {
				stats.addAll(activateOrderInAuctionState(activatedOrder));
			} else {
				throw new UnknownSecurityStateException();
			}
		}

		return stats;
	}

	private List<SecurityStats> activateOrderInContinuousState(Order activatedOrder, long requestId) {
		MatchResult result = matcher.continuousExecuting(activatedOrder, orderBook);
		updateLastTradePrice(result.trades());
		if(!result.trades().isEmpty()) {
			return List.of(ExecuteStats.createContinuousExecuteStatsForActivatedOrder(result.trades(), activatedOrder.getOrderId(), requestId));
		} else {
			return List.of();
		}
	}

	private List<SecurityStats> activateOrderInAuctionState(Order activatedOrder) {
		creditControl.updateCreditForBeingQueued(activatedOrder);
		orderBook.enqueue(activatedOrder);
		return List.of();
	}

	public Order findByOrderId(Side side, long orderId) {
		return orderBook.findByOrderId(side, orderId);
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		return orderBook.isThereOrderWithId(side, orderId);
	}

	public boolean isStopLimitOrder(Side side, long orderId) {
		Order order = orderBook.findByOrderId(side, orderId);
		return (order instanceof StopLimitOrder);
	}
	private AuctionStats createAuctionStats() {
		int openingPrice = matcher.calcOpeningAuctionPrice(orderBook, lastTradePrice);
		int tradableQuantity = matcher.calcTradableQuantity(orderBook, openingPrice);
		return AuctionStats.createAuctionStats(openingPrice, tradableQuantity);
	}
}
