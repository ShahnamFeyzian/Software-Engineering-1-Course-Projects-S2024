package ir.ramtung.tinyme.domain.service.controls;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Trade;

public abstract class MatchingControl {
    protected PositionControl positionControl;
    protected CreditControl creditControl;
    protected QuantityControl quantityControl;
   
    public MatchingControl(PositionControl positionControl, CreditControl creditControl, QuantityControl quantityControl) {
        this.positionControl = positionControl;
        this.creditControl = creditControl;
        this.quantityControl = quantityControl;
    }

    public ControlResult checkBeforeMatching(Order targetOrder, OrderBook orderBook) {
        return ControlResult.OK;
    }

    public void actionAtBeforeMatching(Order targetOrder, OrderBook orderBook) {

    }

    public void actionAtFailedBeforeMatching(Order targetOrder, OrderBook orderBook) {
        
    }

    public ControlResult checkBeforeMatch(Trade trade) {
        return ControlResult.OK;
    }

    public void actionAtMatch(Trade trade, OrderBook orderBook) {
        creditControl.updateCreditsAtTrade(trade);
        quantityControl.updateQuantitiesAtTrade(trade);
        positionControl.updatePositionsAtTrade(trade);
    }

    public void actionAtFailedBeforeMatch(List<Trade> trades, OrderBook orderBook) {
        
    }

    public ControlResult checkAfterMatching(Order targetOrder, List<Trade> trades) {
        return ControlResult.OK;
    }

    public void actionAtAfterMatching(Order targetOrder, OrderBook orderBook) {
        
    }

    public void actionAtfailedAfterMatching(List<Trade> trades, OrderBook orerrBook) {
        
    }
}
