package ir.ramtung.tinyme.domain.service.security_state;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.SecurityState;
import ir.ramtung.tinyme.domain.entity.stats.SecurityStats;

public interface SecurityBehave {
    public List<SecurityStats> addNewOrder(Order newOrder, OrderBook orderBook, int lastTradePrice);
    public List<SecurityStats> updateOrder(Order tempOrder, Order mainOrder, OrderBook orderBook, int lastTradePrice);
    public List<SecurityStats> deleteOrder(Order targetOrder, OrderBook orderBook, int lastTradePrice);
    public List<SecurityStats> activateStopLimitOrders(OrderBook orderBook, int lastTradePrice);
    public List<SecurityStats> changeMatchingState(OrderBook orderBook, int lastTradePrice, SecurityState newState);
}
