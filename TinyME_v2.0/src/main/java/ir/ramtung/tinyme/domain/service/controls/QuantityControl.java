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

    public void updateQuantitiesAtTrade(Trade trade) {
        updateBuyQuantityAtTrade(trade);
        updateSellQuantityAtTrade(trade);
    }

    public void updateQuantitiesAtRollbackTrade(Trade trade) {
        updateBuyQuantityAtRollbackTrade(trade);
        updateSellQuantityAtRollbackTrade(trade);
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

    private void updateBuyQuantityAtTrade(Trade trade) {
        // FIXME: need refactoring
        Order buyOrder = trade.getBuy();
        int tradeQuantity = trade.getQuantity();

        buyOrder.decreaseQuantity(tradeQuantity);
    }

    private void updateSellQuantityAtTrade(Trade trade) {
        // FIXME: need refactoring
        Order sellOrder = trade.getSell();
        int tradeQuantity = trade.getQuantity();

        sellOrder.decreaseQuantity(tradeQuantity);
    }

    private void updateBuyQuantityAtRollbackTrade(Trade trade) {
        // FIXME: need refactoring
        Order buyOrder = trade.getBuy();
        Order orginalBuyOrder = trade.getBuyFirstVersion();

        buyOrder.rollback(orginalBuyOrder);
    }

    private void updateSellQuantityAtRollbackTrade(Trade trade) {
        // FIXME: need refactoring
        Order sellOrder = trade.getSell();
        Order originalSellOrder = trade.getSellFirstVersion();
        
        sellOrder.rollback(originalSellOrder);
    }
}
