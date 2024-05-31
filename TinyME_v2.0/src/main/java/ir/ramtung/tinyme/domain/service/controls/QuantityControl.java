package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Trade;

@Service
public class QuantityControl {
    public ControlResult checkMinimumExecutionQuantity(Order order, List<Trade> trades) {
        if (!order.isNew()) {
            return ControlResult.OK;
        }
        
        int executedQuantity = calcExecutedQuantity(trades);
        if (order.isMinimumExecuteQuantitySatisfied(executedQuantity)) {
            return ControlResult.OK;
        }
        else {
            return ControlResult.NOT_ENOUGH_EXECUTION;
        }
    }

    public void updateQuantitiesAtTrade(Trade trade, OrderBook orderBook) {
        updateBuyQuantityAtTrade(trade, orderBook);
        updateSellQuantityAtTrade(trade, orderBook);
    }

    public void updateQuantitiesAtRollbackTrade(Trade trade, OrderBook orderBook) {
        updateBuyQuantityAtRollbackTrade(trade, orderBook);
        updateSellQuantityAtRollbackTrade(trade, orderBook);
    }

    public void enqueueOrderToOrderBook(Order targetOrder, OrderBook orderBook) {
        if (targetOrder.getQuantity() != 0) {
            orderBook.enqueue(targetOrder);
        }
    }

    private int calcExecutedQuantity(List<Trade> trades) {
		int executedQuantity = 0;
		for (Trade trade : trades) executedQuantity += trade.getQuantity();
		return executedQuantity;
	}

    private void updateBuyQuantityAtTrade(Trade trade, OrderBook orderBook) {
        Order buyOrder = trade.getBuy();
        int tradeQuantity = trade.getQuantity();

        buyOrder.decreaseQuantity(tradeQuantity);
        checkQuantityForUnqueue(buyOrder, orderBook);
    }

    private void updateSellQuantityAtTrade(Trade trade, OrderBook orderBook) {
        Order sellOrder = trade.getSell();
        int tradeQuantity = trade.getQuantity();

        sellOrder.decreaseQuantity(tradeQuantity);
        checkQuantityForUnqueue(sellOrder, orderBook);
    }

    private void checkQuantityForUnqueue(Order order, OrderBook orderBook) {
        if (order.isDone()) {
            orderBook.removeOrder(order);
        }
    }

    private void updateBuyQuantityAtRollbackTrade(Trade trade, OrderBook orderBook) {
        Order buyOrder = trade.getBuy();
        Order orginalBuyOrder = trade.getBuyFirstVersion();

        checkQuantityForEnqueue(buyOrder, orderBook);
        buyOrder.rollback(orginalBuyOrder);
    }

    private void updateSellQuantityAtRollbackTrade(Trade trade, OrderBook orderBook) {
        Order sellOrder = trade.getSell();
        Order originalSellOrder = trade.getSellFirstVersion();
        
        checkQuantityForEnqueue(sellOrder, orderBook);
        sellOrder.rollback(originalSellOrder);
    }

    private void checkQuantityForEnqueue(Order order, OrderBook orderBook) {
        if (order.isDone()) {
            orderBook.enqueue(order);
        }
    }
}
