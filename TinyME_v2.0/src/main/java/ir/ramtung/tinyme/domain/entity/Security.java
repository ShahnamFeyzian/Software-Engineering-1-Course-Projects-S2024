package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.entity.security_stats.AuctionStats;
import ir.ramtung.tinyme.domain.entity.security_stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SituationalStats;
import ir.ramtung.tinyme.domain.entity.security_stats.StateStats;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotEnoughPositionException;
import ir.ramtung.tinyme.domain.exception.UnknownSecurityStateException;
import ir.ramtung.tinyme.domain.service.Matcher;
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

	private static Matcher matcher = new Matcher();

	@Builder.Default
	private SecurityState state = SecurityState.CONTINUOUES;

	public SecurityResponse addNewOrder(Order newOrder) {
		try {
			checkPositionForNewOrder(newOrder);
			List<SecurityStats> stats = handleAdd(newOrder);
			return new SecurityResponse(stats);
		} catch (NotEnoughPositionException exp) {
			return new SecurityResponse(SituationalStats.createNotEnoughPositionsStats(newOrder.getOrderId()));
		} catch (NotEnoughCreditException exp) {
			return new SecurityResponse(SituationalStats.createNotEnoughCreditStats(newOrder.getOrderId()));
		}
	}

	private List<SecurityStats> handleAdd(Order newOrder) {
		if (this.state == SecurityState.CONTINUOUES) {
			return handleAddInContinuesState(newOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return handleAddInAuctionState(newOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> handleAddInAuctionState(Order newOrder) {
		orderBook.enqueue(newOrder);

		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createAddOrderStats(newOrder.getOrderId()));
		stats.add(createAuctionStats());
		return stats;
	}

	private List<SecurityStats> handleAddInContinuesState(Order newOrder) {
		List<SecurityStats> stats = new ArrayList<>();
		if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
			stats.addAll(addNewStopLimitOrder(newStopLimitOrder));
		} else {
			stats.addAll(addNewLimitOrderInContinuesState(newOrder));
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
		orderBook.enqueue(newOrder);
		return List.of(SituationalStats.createAddOrderStats(newOrder.getOrderId()));
	}

	private List<SecurityStats> addNewLimitOrderInContinuesState(Order newOrder) {
		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createAddOrderStats(newOrder.getOrderId()));

		MatchResult newOrderMatchResult = matcher.continuesExecuting(newOrder);
		if (!newOrderMatchResult.isSuccessful()) {
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(newOrderMatchResult, newOrder.getOrderId()));
		}
		if(!newOrderMatchResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuesExecuteStats(newOrderMatchResult.trades(), newOrder.getOrderId()));
		}
		updateLastTradePrice(newOrderMatchResult.trades());
		return stats;
	}

	private void checkPositionForNewOrder(Order newOrder) {
		if (newOrder.getSide() == Side.BUY) {
			return;
		}

		Shareholder shareholder = newOrder.getShareholder();
		int salesAmount = newOrder.getQuantity();
		int queuedPositionAmount = orderBook.totalSellQuantityByShareholder(shareholder);
		int totalNeededPosition = salesAmount + queuedPositionAmount;

		if (!shareholder.hasEnoughPositionsOn(this, totalNeededPosition)) {
			throw new NotEnoughPositionException();
		}
	}

	public SecurityResponse deleteOrder(Side side, long orderId) {
		orderBook.removeByOrderId(side, orderId);

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
		if (prevState == SecurityState.CONTINUOUES) {
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

		List<Trade> trades = matcher.auctionExecuting(orderBook, lastTradePrice);
		if (!trades.isEmpty()) {
			stats.add(ExecuteStats.createAuctionExecuteStats(trades));
		}

		updateLastTradePrice(trades);

		return stats;
	}

	public SecurityResponse updateOrder(Order tempOrder) {
		try {
			Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
			checkPositionForUpdateOrder(mainOrder, tempOrder);
			List<SecurityStats> stats = handleUpdate(tempOrder, mainOrder);
			return new SecurityResponse(stats);
		} catch (NotEnoughPositionException exp) {
			return new SecurityResponse(SituationalStats.createNotEnoughPositionsStats(tempOrder.getOrderId()));
		}
	}

	private List<SecurityStats> handleUpdate(Order tempOrder, Order mainOrder) {
		boolean losesPriority = mainOrder.willPriorityLostInUpdate(tempOrder);
		if (losesPriority) {
			Order originalOrder = mainOrder.snapshot();
			orderBook.removeByOrderId(originalOrder.getSide(), originalOrder.getOrderId());
			mainOrder.updateFromTempOrder(tempOrder);
			return reAddUpdatedOrder(mainOrder, originalOrder);
		} else {
			return updateByKeepingPriority(tempOrder, mainOrder);
		}
	}

	private List<SecurityStats> updateByKeepingPriority(Order tempOrder, Order mainOrder) {
		if (this.state == SecurityState.CONTINUOUES) {
			return updateByKeepingPriorityInContinuesState(tempOrder, mainOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return updateByKeepingPriorityInAuctionState(tempOrder, mainOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> updateByKeepingPriorityInContinuesState(Order tempOrder, Order mainOrder) {
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
		if (this.state == SecurityState.CONTINUOUES) {
			return reAddUpdatedOrderInContinuesState(updatedOrder, originalOrder);
		} else if (this.state == SecurityState.AUCTION) {
			return reAddUpdatedOrderInAuctionState(updatedOrder, originalOrder);
		} else {
			throw new UnknownSecurityStateException();
		}
	}

	private List<SecurityStats> reAddUpdatedOrderInContinuesState(Order updatedOrder, Order originalOrder) {
		if (updatedOrder instanceof StopLimitOrder updatedSlo) {
			StopLimitOrder originalSlo = (StopLimitOrder) originalOrder;
			return reAddUpdatedSloInContinuesState(updatedSlo, originalSlo);
		} else {
			return reAddActiveOrderInContinuesState(updatedOrder, originalOrder);
		}
	}

	private List<SecurityStats> reAddUpdatedOrderInAuctionState(Order updatedOrder, Order originalOrder) {
		try {
			orderBook.enqueue(updatedOrder);
			List<SecurityStats> stats = new LinkedList<>();
			stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
			stats.add(createAuctionStats());
			return stats;
		} catch (NotEnoughCreditException e) {
			orderBook.enqueue(originalOrder);
			return List.of(SituationalStats.createNotEnoughCreditStats(originalOrder.getOrderId()));
		}
	}

	private List<SecurityStats> reAddActiveOrderInContinuesState(Order updatedOrder, Order originalOrder) {
		List<SecurityStats> stats = new LinkedList<>();
		stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
		
		MatchResult updatedOrderResult = matcher.continuesExecuting(updatedOrder);

		if (!updatedOrderResult.isSuccessful()) {
			orderBook.enqueue(originalOrder);
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(updatedOrderResult, originalOrder.getOrderId()));
		} 
		if (!updatedOrderResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuesExecuteStats(updatedOrderResult.trades(), originalOrder.getOrderId()));
		}

		updateLastTradePrice(updatedOrderResult.trades());
		stats.addAll(activateStopLimitOrders());
		return stats;
	}

	private List<SecurityStats> reAddUpdatedSloInContinuesState(StopLimitOrder updatedOrder,StopLimitOrder originalOrder) {
		try {
			List<SecurityStats> stats = new LinkedList<>();
			stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
			orderBook.enqueue(updatedOrder);
			stats.addAll(activateStopLimitOrders());
			return stats;
		} catch (NotEnoughCreditException exp) {
			orderBook.enqueue(originalOrder);
			return List.of(SituationalStats.createNotEnoughCreditStats(originalOrder.getOrderId()));
		}
	}

	private void checkPositionForUpdateOrder(Order mainOrder, Order tempOrder) {
		if (mainOrder.getSide() == Side.BUY) return;

		Shareholder shareholder = mainOrder.getShareholder();
		int pervSalesAmount = mainOrder.getTotalQuantity();
		int newSalesAmount = tempOrder.getTotalQuantity();
		int queuedPositionAmount = orderBook.totalSellQuantityByShareholder(shareholder);
		int totalNeededPosition = newSalesAmount + queuedPositionAmount - pervSalesAmount;

		if (!shareholder.hasEnoughPositionsOn(this, totalNeededPosition)) {
			throw new NotEnoughPositionException();
		}
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
			stats.add(SituationalStats.createOrderActivatedStats(slo.getOrderId()));
			Order activatedOrder = new Order(slo);
			if (this.state == SecurityState.CONTINUOUES) {
				stats.addAll(activateOrderInContinuesState(activatedOrder));
			} else if (this.state == SecurityState.AUCTION) {
				stats.addAll(activateOrderInAuctionState(activatedOrder));
			} else {
				throw new UnknownSecurityStateException();
			}
		}

		return stats;
	}

	private List<SecurityStats> activateOrderInContinuesState(Order activatedOrder) {
		MatchResult result = matcher.continuesExecuting(activatedOrder);
		updateLastTradePrice(result.trades());
		if(!result.trades().isEmpty()) {
			return List.of(ExecuteStats.createContinuesExecuteStats(result.trades(), activatedOrder.getOrderId()));
		} else {
			return List.of();
		}
	}

	private List<SecurityStats> activateOrderInAuctionState(Order activatedOrder) {
		orderBook.enqueue(activatedOrder);
		return List.of();
	}

	public Order findByOrderId(Side side, long orderId) {
		return orderBook.findByOrderId(side, orderId);
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		return orderBook.isThereOrderWithId(side, orderId);
	}

	private AuctionStats createAuctionStats() {
		int openingPrice = matcher.calcOpeningAuctionPrice(orderBook, lastTradePrice);
		int tradableQuantity = matcher.calcTradableQuantity(orderBook, openingPrice);
		return AuctionStats.createAuctionStats(openingPrice, tradableQuantity);
	}
}
