package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
public class IcebergOrder extends Order {
    int peakSize;
    int displayedQuantity;

    public IcebergOrder(long orderId,
                        Security security,
                        Side side,
                        int initialQuantity,
                        int quantity,
                        int price,
                        Broker broker,
                        Shareholder shareholder,
                        LocalDateTime entryTime,
                        int peakSize,
                        int displayedQuantity,
                        int minimumExecutionQuantity,
                        OrderStatus status) {
        super(orderId, security, side, initialQuantity, quantity, price, broker, shareholder, entryTime, minimumExecutionQuantity, status);
        this.peakSize = peakSize;
        this.displayedQuantity = displayedQuantity;
    }

    public IcebergOrder(long orderId,
                        Security security,
                        Side side,
                        int initialQuantity,
                        int quantity,
                        int price,
                        Broker broker,
                        Shareholder shareholder,
                        LocalDateTime entryTime,
                        int peakSize,
                        int minimumExecutionQuantity,
                        OrderStatus status) {
        this(orderId, security, side, initialQuantity, quantity, price, broker, shareholder, entryTime, peakSize, Math.min(peakSize, quantity), minimumExecutionQuantity, status);
    }

    public IcebergOrder(long orderId,
                        Security security,
                        Side side,
                        int quantity,
                        int price,
                        Broker broker,
                        Shareholder shareholder,
                        LocalDateTime entryTime,
                        int peakSize,
                        int minimumExecutionQuantity) {
        this(orderId, security, side, quantity, quantity, price, broker, shareholder, entryTime, peakSize, minimumExecutionQuantity, OrderStatus.NEW);
    }

    public IcebergOrder(long orderId,
                        Security security,
                        Side side,
                        int quantity,
                        int price,
                        Broker broker,
                        Shareholder shareholder,
                        int peakSize,
                        int minimumExecutionQuantity) {
        super(orderId, security, side, quantity, price, broker, shareholder, minimumExecutionQuantity);
        this.peakSize = peakSize;
        this.displayedQuantity = Math.min(peakSize, quantity);
    }

    public IcebergOrder(long orderId,
                        Security security,
                        Side side,
                        int quantity,
                        int price,
                        Broker broker,
                        Shareholder shareholder,
                        int peakSize) {
        super(orderId, security, side, quantity, price, broker, shareholder, 0);
        this.peakSize = peakSize;
        this.displayedQuantity = Math.min(peakSize, quantity);
    }

    @Override
    public Order snapshot() {
        return new IcebergOrder(orderId,
                security,
                side,
                initialQuantity,
                quantity,
                price,
                broker,
                shareholder,
                entryTime,
                peakSize,
                minimumExecutionQuantity,
                OrderStatus.SNAPSHOT);
    }

    @Override
    public Order snapshotWithQuantity(int newQuantity) {
        return new IcebergOrder(orderId,
                security,
                side,
                initialQuantity,
                newQuantity,
                price,
                broker,
                shareholder,
                entryTime,
                peakSize,
                minimumExecutionQuantity,
                OrderStatus.SNAPSHOT);
    }

    @Override
    public int getQuantity() {
        if (status == OrderStatus.NEW)
            return super.getQuantity();
        return displayedQuantity;
    }

    @Override
    public void decreaseQuantity(int amount) {
        if (status == OrderStatus.NEW) {
            super.decreaseQuantity(amount);
            return;
        }
        if (amount > displayedQuantity)
            throw new IllegalArgumentException();
        quantity -= amount;
        displayedQuantity -= amount;
    }

    public void replenish() {
        displayedQuantity = Math.min(quantity, peakSize);
    }

    @Override
    public boolean minimumExecutionQuantitySatisfied() {
        displayedQuantity = Math.min(quantity, peakSize);
        return super.minimumExecutionQuantitySatisfied();
    }

    @Override
    public void updateFromRequest(EnterOrderRq updateOrderRq) {
        super.updateFromRequest(updateOrderRq);
        if (peakSize < updateOrderRq.getPeakSize()) {
            displayedQuantity = Math.min(quantity, updateOrderRq.getPeakSize());
        } else if (peakSize > updateOrderRq.getPeakSize()) {
            displayedQuantity = Math.min(displayedQuantity, updateOrderRq.getPeakSize());
        }
        peakSize = updateOrderRq.getPeakSize();
    }
}
