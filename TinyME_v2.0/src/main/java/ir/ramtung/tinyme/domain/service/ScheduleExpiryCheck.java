package ir.ramtung.tinyme.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Security;

public class ScheduleExpiryCheck extends TimerTask {
    private final List<Order> todayOrders;

    public ScheduleExpiryCheck(List<Order> todayOrders) {
        super();
        this.todayOrders = todayOrders;
    }

    @Override
    public void run() {
        for(int i = 0; i < todayOrders.size(); i++) {
            if (LocalDateTime.now().isBefore(todayOrders.get(i).getExpiryDate())) {
                Order order = todayOrders.get(i);
                todayOrders.remove(i);
                Security security = order.getSecurity();
                security.deleteOrder(order.getSide(), order.getOrderId());
                i--;
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(todayOrders);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }   
        if (!(other instanceof ScheduleExpiryCheck)) {
            return false;
        }
        ScheduleExpiryCheck expiryCheck = (ScheduleExpiryCheck) other;
        return expiryCheck.todayOrders.equals(this.todayOrders);
    }
}
