package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Trade;

@Service
public class MatchingControl {
    private PositionControl positionControl;
    private CreditControl creditControl;

    public MatchingControl(PositionControl positionControl, CreditControl creditControl) {
        this.positionControl = positionControl;
        this.creditControl = creditControl;
    }

    public ControlResult startContinuousExecuting(Order newOrder, OrderBook orderBook) {
        return positionControl.checkPositionForOrder(newOrder, orderBook);
    }

    public ControlResult beforeTradeAtContinuousExecuting(Order newOrder, Order matchingOrder) {
        return creditControl.chekCreditForContinousMatching(newOrder, matchingOrder);
    }

    public ControlResult endContinuousExecuting(Order newOrder, List<Trade> trades) {
        return creditControl.checkCreditForBeQueued(newOrder);
    }
}
