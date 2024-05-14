package ir.ramtung.tinyme.domain.entity;

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

	public Order getLowestPriorityActiveOrder(Side side) {
		if (side == Side.BUY) {
			return buyQueue.getLast();
		} else {
			return sellQueue.getLast();
		}
	}

	public void enqueue(Order order) {
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

	private LinkedList<Order> getQueue(Side side) {
		return side == Side.BUY ? buyQueue : sellQueue;
	}

	private List<Order> getQueue(Order order) {
		if (order instanceof StopLimitOrder) {
			return (order.getSide() == Side.BUY) ? stopLimitOrderBuyQueue : stopLimitOrderSellQueue;
		} else {
			return (order.getSide() == Side.BUY) ? buyQueue : sellQueue;
		}
	}

	public Order findByOrderId(Side side, long orderId) {
		List<Order> queue = (side == Side.BUY) ? buyQueue : sellQueue;
		Order order = searchForOrderInQueue(side, orderId, queue);
		if (order != null) {
			return order;
		}

		queue = (side == Side.BUY) ? stopLimitOrderBuyQueue : stopLimitOrderSellQueue;
		order = searchForOrderInQueue(side, orderId, queue);

		if (order == null) {
			throw new NotFoundException();
		}
		return order;
	}

	private Order searchForOrderInQueue(Side side, long orderId, List<Order> queue) {
		for (Order order : queue) {
			if (order.getOrderId() == orderId) {
				return order;
			}
		}
		return null;
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		try {
			findByOrderId(side, orderId);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
	}

	public void removeByOrderId(Side side, long orderId) {
		Order targetOrder = findByOrderId(side, orderId);
		List<Order> queue = getQueue(targetOrder);
		targetOrder.delete();
		queue.remove(targetOrder);
	}

	public Order findOrderToMatchWith(Order newOrder) {
		var queue = getQueue(newOrder.getSide().opposite());

		if (newOrder.matches(queue.getFirst())) {
			return queue.getFirst();
		}

		throw new NotFoundException();
	}

	public void putBack(Order order) {
		LinkedList<Order> queue = getQueue(order.getSide());
		order.queue();
		queue.addFirst(order);
	}

	public void restoreSellOrder(Order sellOrder) {
		removeByOrderId(Side.SELL, sellOrder.getOrderId());
		putBack(sellOrder);
	}

	public boolean hasOrderOfType(Side side) {
		return !getQueue(side).isEmpty();
	}

	public void removeFirst(Side side) {
		getQueue(side).removeFirst();
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

	private StopLimitOrder findSatisfiedStopLimitOrderBuyQueue(int lastTradePrice) {
		return findSatisfiedStopLimitOrder(stopLimitOrderBuyQueue, lastTradePrice);
	}

	private StopLimitOrder findSatisfiedStopLimitOrderSellQueue(int lastTradePrice) {
		return findSatisfiedStopLimitOrder(stopLimitOrderSellQueue, lastTradePrice);
	}

	public StopLimitOrder getStopLimitOrder(int lastTradePrice) {
		StopLimitOrder sloOrder = findSatisfiedStopLimitOrderBuyQueue(lastTradePrice);

		if (sloOrder == null) {
			sloOrder = findSatisfiedStopLimitOrderSellQueue(lastTradePrice);
		}

		return sloOrder;
	}

	private StopLimitOrder findSatisfiedStopLimitOrder(List<Order> queue, int lastTradePrice) {
		if (queue.isEmpty()) {
			return null;
		}

		StopLimitOrder sloOrder = (StopLimitOrder) queue.getFirst();
		if (sloOrder.isSatisfied(lastTradePrice)) {
			sloOrder.delete();
			queue.remove(sloOrder);
			return sloOrder;
		}
		return null;
	}
}
