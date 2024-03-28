package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;

import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class Matcher {
    public List<Trade> match(Order newOrder) {
        OrderBook orderBook = newOrder.getSecurity().getOrderBook();
        LinkedList<Trade> trades = new LinkedList<>();
        try {
            while (hasOrderToMatch(newOrder, orderBook)) {
                Order matchingOrder = orderBook.findOrderToMatchWith(newOrder);    
                Trade trade = new Trade(newOrder, matchingOrder);
                trade.confirm();
                trades.add(trade);
            }
            return trades;
        }
        catch (NotFoundException exp) {
            return trades;
        } 
        catch (NotEnoughCreditException exp) {
            rollbackTrades(trades);
            throw exp;
        }
    }

    private boolean hasOrderToMatch(Order newOrder, OrderBook orderBook) {
        return (orderBook.hasOrderOfType(newOrder.getSide().opposite())) && (newOrder.getQuantity() > 0);
    }

    private void rollbackTrades(List<Trade> trades) {
        for (int i = trades.size() - 1; i >= 0; i--)
            trades.get(i).rollback();
    }

    public MatchResult execute(Order order) {
        try {
            List<Trade> trades = match(order);
            addOrderToQueue(order, trades);
            return MatchResult.executed(order, trades);
        }
        catch (NotEnoughCreditException exp) {
            return MatchResult.notEnoughCredit();
        }
    }

    private void addOrderToQueue(Order order, List<Trade> trades) {
        if (order.getTotalQuantity() == 0)
            return;
        try {
            order.getSecurity().getOrderBook().enqueue(order);   
        }
        catch (NotEnoughCreditException exp) {
            rollbackTrades(trades);
            throw exp;
        }
        // TODO
        // this is just painkiller, it should be treated properly
    }
}
