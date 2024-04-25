package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import lombok.Getter;

@Getter
public class OrderBook {

	private final LinkedList<Order> buyQueue;
	private final LinkedList<Order> sellQueue;
	private final LinkedList<Order> stopLimitOrderSellQueue;
	private final LinkedList<Order> stopLimitOrderBuyQueue;

	public OrderBook() {
		buyQueue = new LinkedList<>();
		sellQueue = new LinkedList<>();
		stopLimitOrderSellQueue = new LinkedList<>();
		stopLimitOrderBuyQueue = new LinkedList<>();
	}


	public void enqueueStopLimitOrder(StopLimitOrder order) {
		if (
				order.getSide() == Side.BUY && !order.getBroker().hasEnoughCredit(order.getValue())
		) throw new NotEnoughCreditException();

		List<Order> queue = getQueue(order);
		ListIterator<Order> it = queue.listIterator();
		while (it.hasNext()) {
			if (order.queuesBefore(it.next())) {
				it.previous();
				break;
			}
		}
		order.queue();
		it.add(order);
	}

	public void enqueue(Order order) {
		if(order instanceof StopLimitOrder) {
			enqueueStopLimitOrder((StopLimitOrder) order);
			return;
		}

		if (order.getSide() == Side.BUY && order.getStatus() != OrderStatus.LOADING) order
				.getBroker()
				.decreaseCreditBy(order.getValue());


		List<Order> queue = getQueue(order);
		ListIterator<Order> it = queue.listIterator();
		while (it.hasNext()) {
			if (order.queuesBefore(it.next())) {
				it.previous();
				break;
			}
		}
		order.queue();
		it.add(order);
	}


	private LinkedList<Order> getQueue(Order order) {
		if(order instanceof StopLimitOrder) {
			return (order.side == Side.BUY) ? stopLimitOrderBuyQueue : stopLimitOrderSellQueue;
		} else {
			return (order.side == Side.BUY) ? buyQueue : sellQueue;
		}
	}

	// FIXME: fix me lotfan *_* -><- ?
	private LinkedList<Order> getOrderQueue(Side side) {
		return (side == Side.BUY) ? buyQueue : sellQueue;
	}
	private LinkedList<Order> getSlOrderQueue(Side side) {
		return (side == Side.BUY) ? stopLimitOrderBuyQueue : stopLimitOrderSellQueue;
	}

	private Order findOrderInQueue(LinkedList<Order> queue, long orderId) {
		for (Order order : queue) {
			if (order.getOrderId() == orderId) return order;
		}
		throw new NotFoundException();
	}

	public Order findOrder(Side side, long orderId) {
		var queue = getOrderQueue(side);
		return findOrderInQueue(queue, orderId);
	}

	public Order findSlOrder(Side side, long orderId) {
		var queue = getSlOrderQueue(side);
		return findOrderInQueue(queue, orderId);
	}


	public boolean isThereOrderWithId(Side side, long orderId) {
		try {
			findOrder(side, orderId);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
	}

	public boolean isThereSlOrderWithId(Side side, long orderId) {
		try {
			findSlOrder(side, orderId);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
	}

	private void removeFromQueue(LinkedList<Order> queue, Order order) {
		order.delete();
		queue.remove(order);
	}
	public void removeByOrderId(Side side, long orderId) {
		try {
			var queue = getOrderQueue(side);
			Order targetOrder = findByOrderId(side, orderId);
			removeFromQueue(queue, targetOrder);
		} catch (NotFoundException exp) {
			var queue = getSlOrderQueue(side);
			Order targetOrder = findByOrderId(side, orderId);
			removeFromQueue(queue, targetOrder);
		}
	}

	public Order findOrderToMatchWith(Order newOrder) {
		var queue = getOrderQueue(newOrder.getSide().opposite());
		if (newOrder.matches(queue.getFirst())) return queue.getFirst(); else throw new NotFoundException();
	}

	public void putBack(Order order) {
		LinkedList<Order> queue = getOrderQueue(order.getSide());
		order.queue();
		queue.addFirst(order);
	}

	public void restoreSellOrder(Order sellOrder) {
		removeByOrderId(Side.SELL, sellOrder.getOrderId());
		putBack(sellOrder);
	}

	public boolean hasOrderOfType(Side side) {
		return !getOrderQueue(side).isEmpty();
	}

	public int totalSellQuantityByShareholder(Shareholder shareholder) {
		return (
			sellQueue
				.stream()
				.filter(order -> order.getShareholder().equals(shareholder))
				.mapToInt(Order::getTotalQuantity)
				.sum() +
			stopLimitOrderSellQueue
				.stream()
				.filter(order -> order.getShareholder().equals(shareholder))
				.mapToInt(Order::getTotalQuantity)
				.sum()
		);
	}

	public StopLimitOrder getStopLimitOrder(int lastTradePrice) {
		StopLimitOrder sloOrder = findSatisfiedStopLimitOrder(stopLimitOrderBuyQueue, lastTradePrice);
		if (sloOrder != null) return sloOrder;

		sloOrder = findSatisfiedStopLimitOrder(stopLimitOrderSellQueue, lastTradePrice);
		return sloOrder;
	}

	private StopLimitOrder findSatisfiedStopLimitOrder(List<Order> queue, int lastTradePrice) {
		if (queue.isEmpty()) return null;

		StopLimitOrder sloOrder = (StopLimitOrder) queue.getFirst();
		if (sloOrder.isSatisfied(lastTradePrice)) {
			sloOrder.delete();
			queue.remove(sloOrder);
			return sloOrder;
		}
		return null;
	}
}
