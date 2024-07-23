package ir.ramtung.tinyme.domain.service;

import java.time.Duration;
import java.util.Timer;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;

@Service
public class ExpiringService {
    private Timer timer;

    public ExpiringService() {
        this.timer = new Timer();
    }

    public ExpiringService(Timer timer) {
        this.timer = timer;
    }

    public void scheduleexpiryDate(Order order) {
        long delay = Duration.between(order.getEntryTimes().get(0), order.getExpiryDate()).toSeconds() * 1000;
		timer.schedule(new ScheduleexpiryDate(order.getSecurity(), order.getSide(), order.getOrderId()), delay);
    }
}
