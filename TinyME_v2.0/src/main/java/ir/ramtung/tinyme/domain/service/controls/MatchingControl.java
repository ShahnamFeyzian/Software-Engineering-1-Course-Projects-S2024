package ir.ramtung.tinyme.domain.service.controls;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;

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
}
