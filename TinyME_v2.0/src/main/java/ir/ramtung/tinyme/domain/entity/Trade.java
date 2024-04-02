package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.CantRollbackTradeException;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
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

    public Trade(Security security, int price, int quantity, Order order1, Order order2) {
        this.security = security;
        this.price = price;
        this.quantity = quantity;
        if (order1.getSide() == Side.BUY) {
            this.buy = order1;
            this.sell = order2;
            this.sellFirstVersion = order2.snapshot();
        } else {
            this.buy = order2;
            this.sell = order1;
            this.sellFirstVersion = order1.snapshot();
        }
        // TODO
        // this exists just for unit tests and should remove
    }

    public Trade(Security security, int price, int quantity, Order order1, Order order2, Order sellFirstVersion) {
        this(security, price, quantity, order1, order2);
        this.sellFirstVersion = sellFirstVersion;
    }

    public Trade(Order order1, Order order2) {
        this.security = order1.getSecurity();
        this.price = (order1.getStatus() == OrderStatus.QUEUED) ? order1.getPrice() : order2.getPrice();
        this.quantity = Math.min(order1.getQuantity(), order2.getQuantity());
        if (order1.getSide() == Side.BUY) {
            this.buy = order1;
            this.sell = order2;
            this.sellFirstVersion = order2.snapshot();
        } else {
            this.buy = order2;
            this.sell = order1;
            this.sellFirstVersion = order1.snapshot();
        }
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

    private boolean buyerHasEnoughCredit() {
        return buy.getBroker().hasEnoughCredit(getTradedValue());
    }

    public void confirm() {
        if (buy.getStatus() == OrderStatus.NEW) {
            if (!buyerHasEnoughCredit()) 
                throw new NotEnoughCreditException();
            decreaseBuyersCredit();
        }
        increaseSellersCredit();
        increaseBuyersPosition();
        decreaseSellersPosition();
        buy.decreaseQuantity(quantity);
        sell.decreaseQuantity(quantity);
    }

    public void rollback() {
        if (buy.getStatus() != OrderStatus.NEW)
            throw new CantRollbackTradeException();
    
        increaseBuyersCredit();
        decreaseSellersCredit();
        decreaseBuyersPosition();
        increaseSellersPosition();
        sell.rollback(sellFirstVersion);
    }
}
