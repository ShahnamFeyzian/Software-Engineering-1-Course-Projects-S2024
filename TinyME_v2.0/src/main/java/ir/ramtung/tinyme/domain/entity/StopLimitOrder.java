package ir.ramtung.tinyme.domain.entity;

public class StopLimitOrder extends Order {
    private int stopPrice;

    public StopLimitOrder(long orderId, Security security, Side side, int quantity, int price, Broker broker, Shareholder shareholder, int stopPrice) {
        super(orderId, security, side, quantity, price, broker, shareholder);
        this.stopPrice = stopPrice;
    }
}
