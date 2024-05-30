package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Trade;

@Service
public class CreditControl {
    public ControlResult chekCreditForContinousMatching(Order newOrder, Order matchingOrder) {
        if (newOrder.isSell()) {
            return ControlResult.OK;
        }
        
        int quantity = Math.min(newOrder.getQuantity(), matchingOrder.getQuantity());
        int value = quantity * matchingOrder.getPrice();
        Broker broker = newOrder.getBroker();
        
        if (broker.hasEnoughCredit(value)) {
            return ControlResult.OK;
        }
        else {
            return ControlResult.NOT_ENOUGH_CREDIT;
        }
    }

    public ControlResult checkCreditForBeQueued(Order order) {
        if (order.isSell()) {
            return ControlResult.OK;
        }

        long value = order.getValue();
        Broker broker = order.getBroker();

        if(broker.hasEnoughCredit(value)) {
            return ControlResult.OK;
        }
        else {
            return ControlResult.NOT_ENOUGH_CREDIT;
        }
    }

    public void updateCreditsAtTrade(Trade trade) {
        updateBuyerCreditAtTrade(trade);
        updateSellerCreditAtTrade(trade);
    }

    public void updateCreditsAtRollbackTrade(Trade trade) {
        updateBuyerCreditAtRollbackTrade(trade);
        updateSellerCreditAtRollbackTrade(trade);
    }

    public void updateCreditAfterContinuousMatching(Order targetOrder) {
        // FIXME: need refactoring
        // if (targetOrder.isBuy()) {
        //     Broker buyerBroker = targetOrder.getBroker();
        //     long remainderValue = targetOrder.getValue();
        //     buyerBroker.decreaseCreditBy(remainderValue);
        // }
    }

    private void updateBuyerCreditAtTrade(Trade trade) {
        Order buyOrder = trade.getBuy();
        Broker buyerBroker = buyOrder.getBroker();
        boolean isBuyOrderQueued = buyOrder.isQueued();
        long buyOrderValue = buyOrder.getValue();
        long tradeValue = trade.getTradedValue();

        // FIXME: need refactoring
        if (!isBuyOrderQueued) {
            buyerBroker.decreaseCreditBy(tradeValue);
        } else if (trade.getPrice() < buyOrder.getPrice()) {
            long backCredit = (long) ((buyOrder.getPrice() - trade.getPrice()) * trade.getQuantity());
			buyOrder.getBroker().increaseCreditBy(backCredit);
        }
    }

    private void updateSellerCreditAtTrade(Trade trade) {
        Order sellOrder = trade.getSell();
        Broker sellerBroker = sellOrder.getBroker();
        long tradeValue = trade.getTradedValue();

        sellerBroker.increaseCreditBy(tradeValue);
    }

    private void updateBuyerCreditAtRollbackTrade(Trade trade) {
        Order buyOrder = trade.getBuy();
        Broker buyerBroker = buyOrder.getBroker();
        long tradeValue = trade.getTradedValue();

        // FIXME: need refactoring
        if (!buyOrder.isQueued() || buyOrder.isDone()) {
            buyerBroker.increaseCreditBy(tradeValue);
        }
    }

    private void updateSellerCreditAtRollbackTrade(Trade trade) {
        Order sellOrder = trade.getSell();
        Broker sellerBroker = sellOrder.getBroker();
        long tradeValue = trade.getTradedValue();

        sellerBroker.decreaseCreditBy(tradeValue);
    }
}
