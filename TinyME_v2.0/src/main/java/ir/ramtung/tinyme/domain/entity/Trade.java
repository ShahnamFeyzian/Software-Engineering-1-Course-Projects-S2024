package ir.ramtung.tinyme.domain.entity;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Trade {
    Security security;
    private int price;
    private int quantity;
    private Order buy;
    private Order sell;
    private Order sellFirstVersion;
    private Order buyFirstVersion;
    private boolean isBuyQueued;

    public Trade(Security security, int price, int quantity, Order order1, Order order2) {
        this.security = security;
        this.price = price;
        this.quantity = quantity;
        if (order1.getSide() == Side.BUY) {
            this.buy = order1;
            this.sell = order2;
            this.sellFirstVersion = order2.snapshot();
            this.buyFirstVersion = order1.snapshot();
        } 
        else {
            this.buy = order2;
            this.sell = order1;
            this.sellFirstVersion = order1.snapshot();
            this.buyFirstVersion = order2.snapshot();
        }
        this.isBuyQueued = (this.buy.getStatus() == OrderStatus.QUEUED);
        // TODO
        // this exists just for unit tests and should remove
    }

    public Trade(Security security, int price, int quantity, Order order1, Order order2, Order sellFirstVersion, Order buyFirstVersion, boolean isBuyQueued) {
        this(security, price, quantity, order1, order2);
        this.sellFirstVersion = sellFirstVersion;
        this.buyFirstVersion = buyFirstVersion;
        this.isBuyQueued = isBuyQueued;
    }

    public Trade(Order order1, Order order2) {
        this.security = order1.getSecurity();
        this.price = (order1.getStatus() == OrderStatus.QUEUED) ? order1.getPrice() : order2.getPrice();
        this.quantity = Math.min(order1.getQuantity(), order2.getQuantity());
        if (order1.getSide() == Side.BUY) {
            this.buy = order1;
            this.sell = order2;
            this.sellFirstVersion = order2.snapshot();
            this.buyFirstVersion = order1.snapshot();
        } 
        else {
            this.buy = order2;
            this.sell = order1;
            this.sellFirstVersion = order1.snapshot();
            this.buyFirstVersion = order2.snapshot();
        }
        this.isBuyQueued = (this.buy.getStatus() == OrderStatus.QUEUED);
    }

    public long getTradedValue() {
        return (long) price * quantity;
    }

    private void decreaseSellersCredit() {
        sell.getBroker().decreaseCreditBy(getTradedValue());
    }

    private void increaseSellersCredit() {
        sell.getBroker().increaseCreditBy(getTradedValue());
    }

    private void decreaseBuyersCredit() {
        buy.getBroker().decreaseCreditBy(getTradedValue());
    }

    private void increaseBuyersCredit() {
        buy.getBroker().increaseCreditBy(getTradedValue());
    }

    private void increaseBuyersPosition() {
        buy.getShareholder().incPosition(security, quantity);
    }

    private void decreaseBuyersPosition() {
        buy.getShareholder().decPosition(security, quantity);
    }

    private void increaseSellersPosition() {
        sell.getShareholder().incPosition(security, quantity);
    }

    private void decreaseSellersPosition() {
        sell.getShareholder().decPosition(security, quantity);
    }


    public void confirm() {
        if (!isBuyQueued) {
            decreaseBuyersCredit();
        }
        increaseSellersCredit();
        increaseBuyersPosition();
        decreaseSellersPosition();
        buy.decreaseQuantity(quantity);
        sell.decreaseQuantity(quantity);
    }

    public void rollback() {
        if (isBuyQueued)
            buyQueuedRollback();
        else
            sellQueuedRollback();
    }

    private void sellQueuedRollback() {
        increaseBuyersCredit();
        decreaseSellersCredit();
        decreaseBuyersPosition();
        increaseSellersPosition();
        sell.rollback(sellFirstVersion);
    }
    
    private void buyQueuedRollback() {
        if (buy.getStatus() == OrderStatus.DONE)
            increaseBuyersCredit();
        decreaseSellersCredit();
        decreaseBuyersPosition();
        increaseSellersPosition();
        buy.rollback(buyFirstVersion);
    }
}
