package ir.ramtung.tinyme.domain.entity;

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

	private static  Matcher continuesMatcher = new Matcher();

	public List<MatchResult> addNewOrder(Order newOrder) {
		try {
			List<MatchResult> results = new ArrayList<>();
			checkPositionForNewOrder(newOrder);
			handleAdd(newOrder, results);
			results.addAll(executeStopLimitOrders());
			return results;
		} catch (NotEnoughPositionException exp) {
			return List.of(MatchResult.notEnoughPositions());
		} catch (NotEnoughCreditException exp) {
			return List.of(MatchResult.notEnoughCredit());
		}
	}

	private void handleAdd(Order newOrder, List<MatchResult> results) {
		if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
			addNewStopLimitOrder(newStopLimitOrder);
			results.addFirst(MatchResult.executed(newOrder, List.of()));
		} else {
			MatchResult newOrderMatchResult = continuesMatcher.execute(newOrder);
			updateLastTradePrice(newOrderMatchResult.trades());
			results.addFirst(newOrderMatchResult);
		}
	}

	private void updateLastTradePrice(List<Trade> trades) {
		if (!trades.isEmpty()) {
			lastTradePrice = trades.getLast().getPrice();
		}
	}

	private void addNewStopLimitOrder(StopLimitOrder newOrder) {
		orderBook.enqueue(newOrder);
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

	public void deleteOrder(Side side, long orderId) {
		orderBook.removeByOrderId(side, orderId);
	}

	public List<MatchResult> updateOrder(Order tempOrder) {
		try {
			Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
			checkPositionForUpdateOrder(mainOrder, tempOrder);
			boolean losesPriority = mainOrder.willPriorityLostInUpdate(tempOrder);
			return handleUpdate(tempOrder, mainOrder, losesPriority);
		} catch (NotEnoughPositionException exp) {
			return List.of(MatchResult.notEnoughPositions());
		}
	}

	private List<MatchResult> handleUpdate(Order tempOrder, Order mainOrder, boolean losesPriority) {
		if (losesPriority) {
			Order originalOrder = mainOrder.snapshot();
			orderBook.removeByOrderId(originalOrder.getSide(), originalOrder.getOrderId());
			mainOrder.updateFromTempOrder(tempOrder);
			return reAddUpdatedOrder(mainOrder, originalOrder);
		} else {
			mainOrder.updateFromTempOrder(tempOrder);
			return List.of(MatchResult.executed(null, List.of()));
		}
	}

	private List<MatchResult> reAddUpdatedOrder(Order updatedOrder, Order originalOrder) {
		if (updatedOrder instanceof StopLimitOrder updatedSlo) {
			StopLimitOrder originalSlo = (StopLimitOrder) originalOrder;
			return reAddUpdatedSlo(updatedSlo, originalSlo);
		} else {
			return reAddActiveOrder(updatedOrder, originalOrder);
		}
	}

	private List<MatchResult> reAddActiveOrder(Order updatedOrder, Order originalOrder) {
		MatchResult updatedOrderResult = continuesMatcher.execute(updatedOrder);

		if (updatedOrderResult.outcome() != MatchingOutcome.EXECUTED) {
			orderBook.enqueue(originalOrder);
		}

		updateLastTradePrice(updatedOrderResult.trades());
		List<MatchResult> results = executeStopLimitOrders();
		results.addFirst(updatedOrderResult);
		return results;
	}

	private List<MatchResult> reAddUpdatedSlo(StopLimitOrder updatedOrder,StopLimitOrder originalOrder) {
		try {
			List<MatchResult> results = new LinkedList<>();
			results.add(MatchResult.executed(updatedOrder, List.of()));
			addNewStopLimitOrder(updatedOrder);
			results.addAll(executeStopLimitOrders());
			return results;
		} catch (NotEnoughCreditException exp) {
			addNewStopLimitOrder(originalOrder);
			return List.of(MatchResult.notEnoughCredit());
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

	public List<String> checkLotAndTickSize(EnterOrderRq order) {
		List<String> errors = new LinkedList<>();
		
		if (order.getQuantity() % lotSize != 0) {
			errors.add(Message.QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE);
		}

		if (order.getPrice() % tickSize != 0) {
			errors.add(Message.PRICE_NOT_MULTIPLE_OF_TICK_SIZE);
		}

		return errors;
	}

	private List<MatchResult> executeStopLimitOrders() {
		List<MatchResult> results = new LinkedList<>();
		StopLimitOrder sloOrder;

		while ((sloOrder = orderBook.getStopLimitOrder(lastTradePrice)) != null) {
			Order activatedOrder = new Order(sloOrder);
			MatchResult result = continuesMatcher.execute(activatedOrder);
			updateLastTradePrice(result.trades());
			results.add(result);
		}

		return results;
	}

	public Order findByOrderId(Side side, long orderId) {
		return orderBook.findByOrderId(side, orderId);
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		return orderBook.isThereOrderWithId(side, orderId);
	}
}
