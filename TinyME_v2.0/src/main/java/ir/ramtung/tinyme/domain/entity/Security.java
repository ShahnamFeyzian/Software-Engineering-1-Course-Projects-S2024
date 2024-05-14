package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.entity.security_stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SituationalStats;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotEnoughPositionException;
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
			List<SecurityStats> stats = List.of(SituationalStats.createNotEnoughPositionsStats(newOrder.getOrderId()));
			return new SecurityResponse(stats);
		} catch (NotEnoughCreditException exp) {
			List<SecurityStats> stats = List.of(SituationalStats.createNotEnoughCreditStats(newOrder.getOrderId()));
			return new SecurityResponse(stats);
		}
	}

	private List<SecurityStats> handleAdd(Order newOrder) {
		if (this.state == SecurityState.CONTINUOUES) {
			return handleAddInContinuesState(newOrder);
		} else if (this.state == SecurityState.AUCTION) {
			// TODO: complete this part
			return null;
		} else {
			throw new UnknownError("Unknown security state");
		}
	}

	private List<SecurityStats> handleAddInContinuesState(Order newOrder) {
		List<SecurityStats> stats = new ArrayList<>();
		if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
			stats.addAll(addNewStopLimitOrder(newStopLimitOrder));
		} else {
			stats.addAll(addNewLimitOrderInContinuesState(newOrder));
		}
		stats.addAll(executeStopLimitOrders());
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
		List<SecurityStats> stats = List.of(SituationalStats.createDeleteOrderStats(orderId));
		return new SecurityResponse(stats);
	}

	public void changeMatchingState(SecurityState newState) {
		// TODO: complete this part
		this.state = newState;
	}

	public SecurityResponse updateOrder(Order tempOrder) {
		try {
			Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
			checkPositionForUpdateOrder(mainOrder, tempOrder);
			List<SecurityStats> stats = handleUpdate(tempOrder, mainOrder);
			return new SecurityResponse(stats);
		} catch (NotEnoughPositionException exp) {
			List<SecurityStats> stats = List.of(SituationalStats.createNotEnoughPositionsStats(tempOrder.getOrderId()));
			return new SecurityResponse(stats);
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
			mainOrder.updateFromTempOrder(tempOrder);
			return List.of(SituationalStats.createUpdateOrderStats(mainOrder.getOrderId()));
		}
	}

	private List<SecurityStats> reAddUpdatedOrder(Order updatedOrder, Order originalOrder) {
		if (updatedOrder instanceof StopLimitOrder updatedSlo) {
			StopLimitOrder originalSlo = (StopLimitOrder) originalOrder;
			return reAddUpdatedSlo(updatedSlo, originalSlo);
		} else {
			return reAddActiveOrder(updatedOrder, originalOrder);
		}
	}

	private List<SecurityStats> reAddActiveOrder(Order updatedOrder, Order originalOrder) {
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
		stats.addAll(executeStopLimitOrders());
		return stats;
	}

	private List<SecurityStats> reAddUpdatedSlo(StopLimitOrder updatedOrder,StopLimitOrder originalOrder) {
		try {
			List<SecurityStats> stats = new LinkedList<>();
			stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
			addNewStopLimitOrder(updatedOrder);
			stats.addAll(executeStopLimitOrders());
			return stats;
		} catch (NotEnoughCreditException exp) {
			addNewStopLimitOrder(originalOrder);
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

	private List<SecurityStats> executeStopLimitOrders() {
		List<SecurityStats> stats = new LinkedList<>();
		StopLimitOrder slo;

		while ((slo = orderBook.getStopLimitOrder(lastTradePrice)) != null) {
			stats.add(SituationalStats.createOrderActivatedStats(slo.getOrderId()));
			Order activatedOrder = new Order(slo);
			MatchResult result = matcher.continuesExecuting(activatedOrder);
			if(!result.trades().isEmpty()) {
				stats.add(ExecuteStats.createContinuesExecuteStats(result.trades(), activatedOrder.getOrderId()));
			}
			updateLastTradePrice(result.trades());
		}

		return stats;
	}

	public Order findByOrderId(Side side, long orderId) {
		return orderBook.findByOrderId(side, orderId);
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		return orderBook.isThereOrderWithId(side, orderId);
	}
}
