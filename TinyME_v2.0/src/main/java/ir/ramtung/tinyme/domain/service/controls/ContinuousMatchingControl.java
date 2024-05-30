package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Trade;

@Service
public class ContinuousMatchingControl extends MatchingControl {
    public ContinuousMatchingControl(PositionControl positionControl, CreditControl creditControl, QuantityControl quantityControl) {
        super(positionControl, creditControl, quantityControl);
    }

    @Override
    public ControlResult checkBeforeMatching(Order targetOrder, OrderBook orderBook) {
        return positionControl.checkPositionForOrder(targetOrder, orderBook);
    }

    @Override
    public ControlResult checkBeforeMatch(Trade trade) {
        return creditControl.chekCreditForContinousMatching(trade);
    }

    @Override
    public void actionAtFailedBeforeMatch(List<Trade> trades, OrderBook orderBook) {
        rollbackTrades(trades, orderBook);
    }

    @Override
    public ControlResult checkAfterMatching(Order targetOrder, List<Trade> trades) {
        ControlResult controlResult = quantityControl.checkMinimumExecutionQuantity(targetOrder, trades);
        if (controlResult != ControlResult.OK) {
            return controlResult;
        }

        return creditControl.checkCreditForBeQueued(targetOrder);
    }

    @Override
    public void actionAtAfterMatching(Order targetOrder, OrderBook orderBook) {
        creditControl.updateCreditAfterContinuousMatching(targetOrder);
        quantityControl.updateQuantityAfterContinuousMatching(targetOrder, orderBook);
    }

    @Override
    public void actionAtfailedAfterMatching(List<Trade> trades, OrderBook orerrBook) {
        rollbackTrades(trades, orerrBook);
    }

    private void rollbackTrades(List<Trade> trades, OrderBook orderBook) {
        for (Trade trade : trades.reversed()) {
            creditControl.updateCreditsAtRollbackTrade(trade);
            quantityControl.updateQuantitiesAtRollbackTrade(trade);
            positionControl.updatePositionsAtRollbackTrade(trade);
        }
    }
}
