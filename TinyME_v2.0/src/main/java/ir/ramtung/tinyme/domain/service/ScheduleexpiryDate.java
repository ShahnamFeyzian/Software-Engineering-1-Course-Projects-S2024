package ir.ramtung.tinyme.domain.service;

import java.util.Objects;
import java.util.TimerTask;

import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.domain.entity.Side;

public class ScheduleexpiryDate extends TimerTask {
    private final Security security;
    private final Side orderSide;
    private final long orderId;

    public ScheduleexpiryDate(Security security, Side orderSide, long orderId) {
        super();
        this.security = security;
        this.orderSide = orderSide;
        this.orderId = orderId;
    }

    @Override
    public void run() {
        security.deleteOrder(orderSide, orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(security, orderSide, orderId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }   
        if (!(other instanceof ScheduleexpiryDate)) {
            return false;
        }
        ScheduleexpiryDate scheduleexpiryDate = (ScheduleexpiryDate) other;
        return scheduleexpiryDate.security.equals(this.security) &&
               scheduleexpiryDate.orderSide.equals(this.orderSide) &&
               scheduleexpiryDate.orderId == this.orderId;
    }
}
