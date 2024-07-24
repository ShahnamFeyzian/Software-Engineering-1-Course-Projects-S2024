package ir.ramtung.tinyme.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;

@Service
public class ExpiringService {
    private static final long checkingRate = 1000;
    private Timer timer;
    private ScheduleExpiryCheck expiryCheck;
    private List<Order> todayOrders = new ArrayList<>(); 
    private List<Order> futureOrders = new ArrayList<>(); 

    public ExpiringService() {
        this.timer = new Timer();
        setup();
    }

    public ExpiringService(Timer timer) {
        this.timer = timer;
        setup();
    }

    public void addOrder(Order order) {
        if (todayOrders.contains(order) || futureOrders.contains(order)) {
            return;
        }

        if (isOrderForToday(order)) {
            todayOrders.add(order);
        }
        else if (isOrderForFuture(order)) {
            futureOrders.add(order);
        }
    }

    public void updateOrder(Order order) {
        if (isOrderExpired(order)) {
            deleteOrder(order);
        }
        
        if (isOrderForToday(order) && futureOrders.contains(order)) {
            futureOrders.remove(order);
            todayOrders.add(order);
        }
        else if (isOrderForFuture(order) && todayOrders.contains(order)) {
            todayOrders.remove(order);
            futureOrders.add(order);
        }
    }

    public void deleteOrder(Order order) {
        //TODO: maybe better way to do this !?
        if (todayOrders.contains(order)) {
            todayOrders.remove(order);
        }
        if (futureOrders.contains(order)) {
            futureOrders.remove(order);
        }
    }

    private void updateOrderLists() {
        for (int i = 0; i < futureOrders.size(); i++) {
            Order futureOrder = futureOrders.get(i);
            if (isOrderForToday(futureOrder)) {
                futureOrders.remove(i);
                todayOrders.add(futureOrder);
                i--;
            }
        }
    }

    private boolean isOrderForToday(Order order) {
        return LocalDate.now().equals(order.getExpiryDate().toLocalDate());
    }

    private boolean isOrderForFuture(Order order) {
        return LocalDate.now().isBefore(order.getExpiryDate().toLocalDate());
    }

    private boolean isOrderExpired(Order order) {
        return LocalDate.now().isAfter(order.getExpiryDate().toLocalDate());
    }

    private void setup() {
        expiryCheck = new ScheduleExpiryCheck(todayOrders);
        timer.schedule(expiryCheck, 0, checkingRate);
    }
}
