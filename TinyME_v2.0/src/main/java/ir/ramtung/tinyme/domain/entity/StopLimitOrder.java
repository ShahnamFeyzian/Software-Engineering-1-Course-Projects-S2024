package ir.ramtung.tinyme.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StopLimitOrder extends Order {
    private int stopPrice;

    public StopLimitOrder(long orderId, Security security, Side side, int quantity, int price, Broker broker, Shareholder shareholder, int stopPrice) {
        super(orderId, security, side, quantity, price, broker, shareholder);
        this.stopPrice = stopPrice;
    }

    @Override 
    public boolean queuesBefore(Order order) {
        // TODO
        // some how it can be deleted
        StopLimitOrder sloOrder = (StopLimitOrder) order;
        if (this.side == Side.BUY)
            return stopPrice < sloOrder.getStopPrice();
        else
            return stopPrice > sloOrder.getStopPrice();
    }

    public boolean isSatisfied(int lastTradePrice) {
        if (side == Side.BUY && price <= lastTradePrice)
            return true;
        else if (side == Side.SELL && price >= lastTradePrice)
            return true;
        
        return false;
    }
}
