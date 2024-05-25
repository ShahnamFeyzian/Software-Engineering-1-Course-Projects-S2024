package ir.ramtung.tinyme.domain.service.controls;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;

@Service
public class MatchingControl {
    private PositionControl positionControl;

    public MatchingControl(PositionControl positionControl) {
        this.positionControl = positionControl;
    }

    public ControlResult startContinuousExecuting(Order newOrder, OrderBook orderBook) {
        return positionControl.checkPositionForOrder(newOrder, orderBook);
    }
}
