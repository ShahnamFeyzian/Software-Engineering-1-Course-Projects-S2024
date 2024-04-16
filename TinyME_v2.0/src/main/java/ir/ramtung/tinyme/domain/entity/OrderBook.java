package ir.ramtung.tinyme.domain.entity;

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ir.ramtung.tinyme.domain.exception.NotFoundException;

@Getter
public class OrderBook {
    private final LinkedList<Order> buyQueue;
    private final LinkedList<Order> sellQueue;
    private final LinkedList<StopLimitOrder> stopLimitOrderSellQueue;
    private final LinkedList<StopLimitOrder> stopLimitOrderBuyQueue;

    public OrderBook() {
        buyQueue = new LinkedList<>();
        sellQueue = new LinkedList<>();
        stopLimitOrderSellQueue = new LinkedList<>();
        stopLimitOrderBuyQueue = new LinkedList<>();
    }

    public void enqueueStopLimitOrder(StopLimitOrder order) {
        List<StopLimitOrder> queue = getStopLimitOrderQueue(order.getSide());
        ListIterator<StopLimitOrder> it = queue.listIterator();
        while (it.hasNext()) {
            if (order.queuesBefore(it.next())) {
                it.previous();
                break;
            }
        }
        order.queue();
        it.add(order);
        // TODO 
        // fucking duplication that sould be fixed
    }

    public void enqueue(Order order) {
        if(order.getSide() == Side.BUY && order.getStatus() != OrderStatus.LOADING)
            order.getBroker().decreaseCreditBy(order.getValue());
        
        List<Order> queue =  getQueue(order.getSide());
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

    private LinkedList<StopLimitOrder> getStopLimitOrderQueue(Side side) {
        return (side == Side.BUY) ? stopLimitOrderBuyQueue : stopLimitOrderSellQueue;
    }

    private LinkedList<Order> getQueue(Side side) {
        return side == Side.BUY ? buyQueue : sellQueue;
    }

    public Order findByOrderId(Side side, long orderId) {
        var queue = getQueue(side);
        for (Order order : queue) {
            if (order.getOrderId() == orderId)
                return order;
        }
        throw new NotFoundException();
    }

    public boolean isThereOrderWithId(Side side, long orderId) {
        try {
            findByOrderId(side, orderId);
            return true;
        }
        catch (NotFoundException exp) {
            return false;
        }
    }

    public void removeByOrderId(Side side, long orderId) {
        LinkedList<Order> queue = getQueue(side);
        Order targetOrder = findByOrderId(side, orderId);
        targetOrder.delete();
        queue.remove(targetOrder);
    }

    public Order findOrderToMatchWith(Order newOrder) {
        var queue = getQueue(newOrder.getSide().opposite());
        if (newOrder.matches(queue.getFirst()))
            return queue.getFirst();
        else
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
        return sellQueue.stream()
                .filter(order -> order.getShareholder().equals(shareholder))
                .mapToInt(Order::getTotalQuantity)
                .sum() 
                +
               stopLimitOrderSellQueue.stream()
                .filter(order -> order.getShareholder().equals(shareholder))
                .mapToInt(Order::getTotalQuantity)
                .sum();
    }

    public StopLimitOrder getStopLimitOrder(int lastTradePrice) {
        if (stopLimitOrderBuyQueue.size() != 0) {
            StopLimitOrder firsBuytOrder = stopLimitOrderBuyQueue.get(0);
            if (firsBuytOrder.isSatisfied(lastTradePrice))
                return firsBuytOrder;           
        }
        if (stopLimitOrderSellQueue.size() != 0) {
            StopLimitOrder firsSelltOrder = stopLimitOrderSellQueue.get(0);
            if (firsSelltOrder.isSatisfied(lastTradePrice))
                return firsSelltOrder;           
        }
        return null;
    }
}
