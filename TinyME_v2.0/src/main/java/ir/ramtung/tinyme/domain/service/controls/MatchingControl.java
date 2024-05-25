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
    private ExecutionControl executionControl;

    public MatchingControl(PositionControl positionControl, CreditControl creditControl, ExecutionControl executionControl) {
        this.positionControl = positionControl;
        this.creditControl = creditControl;
        this.executionControl = executionControl;
    }

    public ControlResult startContinuousExecuting(Order targetOrder, OrderBook orderBook) {
        return positionControl.checkPositionForOrder(targetOrder, orderBook);
    }

    public ControlResult beforeTradeAtContinuousExecuting(Order targetOrder, Order matchingOrder) {
        return creditControl.chekCreditForContinousMatching(targetOrder, matchingOrder);
    }

    public ControlResult endContinuousExecuting(Order targetOrder, List<Trade> trades) {
        ControlResult controlResult = executionControl.checkMinimumExecutionQuantity(targetOrder, trades);
        if (controlResult != ControlResult.OK) {
            return controlResult;
        }

        return creditControl.checkCreditForBeQueued(targetOrder);
    }
}
