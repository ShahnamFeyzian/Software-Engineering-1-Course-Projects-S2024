package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
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
        updateBuyerQuantityAtTrade(trade);
        updateSellerQuantityAtTrade(trade);
    }

    private int calcExecutedQuantity(List<Trade> trades) {
		int executedQuantity = 0;
		for (Trade trade : trades) executedQuantity += trade.getQuantity();
		return executedQuantity;
	}

    private void updateBuyerQuantityAtTrade(Trade trade) {
        Order buyOrder = trade.getBuy();
        int tradeQuantity = trade.getQuantity();

        buyOrder.decreaseQuantity(tradeQuantity);
    }

    private void updateSellerQuantityAtTrade(Trade trade) {
        Order sellOrder = trade.getSell();
        int tradeQuantity = trade.getQuantity();

        sellOrder.decreaseQuantity(tradeQuantity);
    }
}
