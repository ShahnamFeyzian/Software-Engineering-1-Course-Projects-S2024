package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Builder
@EqualsAndHashCode
@ToString
@Getter
public class Order {
    protected long orderId;
    protected Security security;
    protected Side side;
    protected int quantity;
    protected int minimumExecutionQuantity;
    protected int price;
    protected Broker broker;
    protected Shareholder shareholder;
    @Builder.Default
    protected LocalDateTime entryTime = LocalDateTime.now();
    @Builder.Default
    protected OrderStatus status = OrderStatus.NEW;

    public Order(long orderId, Security security, Side side, int quantity, int minimumExecutionQuantity, int price, Broker broker, Shareholder shareholder, 
    LocalDateTime entryTime, OrderStatus status) {
        this.orderId = orderId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.minimumExecutionQuantity = minimumExecutionQuantity;
        this.price = price;
        this.entryTime = entryTime;
        this.broker = broker;
        this.shareholder = shareholder;
        this.status = status;
    }

    public Order(long orderId, Security security, Side side, int quantity, int minimumExecutionQuantity, int price, Broker broker, Shareholder shareholder, 
    LocalDateTime entryTime) {
        this(orderId, security, side, quantity, minimumExecutionQuantity, price, broker, shareholder, entryTime, OrderStatus.NEW);
    }

    public Order(long orderId, Security security, Side side, int quantity, int minimumExecutionQuantity, int price, Broker broker, Shareholder shareholder) {
        this(orderId, security, side, quantity, minimumExecutionQuantity, price, broker, shareholder, LocalDateTime.now());
    }

    public Order(long orderId, Security security, Side side, int quantity, int price, Broker broker, Shareholder shareholder) {
        this(orderId, security, side, quantity, 0, price, broker, shareholder);
    }

    public Order snapshot() {
        return new Order(orderId, security, side, quantity, minimumExecutionQuantity, price, broker, shareholder, entryTime, this.status);
    }

    public Order snapshotWithQuantity(int newQuantity) {
        return new Order(orderId, security, side, newQuantity, minimumExecutionQuantity, price, broker, shareholder, entryTime, this.status);
        // TODO
        // this exists just for unit tests and should remove
    }

    public boolean matches(Order other) {
        if (side == Side.BUY)
            return price >= other.price;
        else
            return price <= other.price;
    }

    public void decreaseQuantity(int amount) {
        if (amount > quantity || amount <= 0)
            throw new IllegalArgumentException();
        
        quantity -= amount;
        if(quantity == 0 && status == OrderStatus.QUEUED) {
            status = OrderStatus.DONE;
            security.deleteOrder(side, orderId);
        }
    }

    public void rollback(Order firstVersion) {
        this.quantity = firstVersion.quantity;
        if (status == OrderStatus.DONE) {
            status = OrderStatus.QUEUED; 
            security.getOrderBook().enqueue(this);
        }   
    }

    public void makeQuantityZero() {
        quantity = 0;
    }

    public boolean queuesBefore(Order order) {
        if (order.getSide() == Side.BUY) {
            return price > order.getPrice();
        } else {
            return price < order.getPrice();
        }
    }

    public void queue() {
        status = OrderStatus.QUEUED;
    }

    public boolean isQuantityIncreased(int newQuantity) {
        return newQuantity > quantity;
    }

    public void updateFromTempOrder(Order tempOrder) {
        if (!this.willPriortyLostInUpdate(tempOrder) && this.side == Side.BUY) {
            broker.increaseCreditBy(this.getValue());
            broker.decreaseCreditBy(tempOrder.getValue());
        }
        else
            this.status = OrderStatus.UPDATING;
        this.quantity = tempOrder.quantity;
        this.price = tempOrder.price;
    }

    public long getValue() {
        return (long)price * quantity;
    }

    public int getTotalQuantity() { return quantity; }

    public void checkNewPeakSize(int peakSize) {
        if (peakSize != 0)
            throw new InvalidPeakSizeException();
    }

    public void checkNewMinimumExecutionQuantity(int minimumExecutionQuantity) {
        if (this.minimumExecutionQuantity != minimumExecutionQuantity)
            throw new UpdateMinimumExecutionQuantityException();
    }

    public void delete() {
        if (side == Side.BUY)
            broker.increaseCreditBy(getValue());
    }

    public boolean willPriortyLostInUpdate(Order tempOrder) {
        return (this.quantity < tempOrder.quantity) || (this.price != tempOrder.price);
    }
}
