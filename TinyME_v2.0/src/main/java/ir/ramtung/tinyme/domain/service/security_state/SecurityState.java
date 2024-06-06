package ir.ramtung.tinyme.domain.service.security_state;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.stats.SecurityStats;

public interface SecurityState {
    public List<SecurityStats> addNewOrder(Order newOrder, OrderBook orderBook);
    public List<SecurityStats> updateOrder(Order tempOrder, Order mainOrder, OrderBook orderBook);
    public List<SecurityStats> deleteOrder(Order targetOrder, OrderBook orderBook);
    public List<SecurityStats> changeMatchingState(OrderBook orderBook);
}
