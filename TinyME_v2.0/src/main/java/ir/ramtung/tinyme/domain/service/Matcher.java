package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotEnoughExecutionException;
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
        List<Trade> trades = new LinkedList<>();
        try {
            trades = match(order);
            order.checkExecutionQuantity(someOfExecutionQuantity(trades));
            order.addYourselfToQueue();
            return MatchResult.executed(order, trades);
        }
        catch (NotEnoughCreditException exp) {
            rollbackTrades(trades);
            return MatchResult.notEnoughCredit();
        }
        catch (NotEnoughExecutionException exp) {
            rollbackTrades(trades);
            return MatchResult.notEnoughExecution();
        }
    }

    private int someOfExecutionQuantity(List<Trade> trades) {
        int quantitySome = 0;
        for (Trade trade: trades) 
            quantitySome += trade.getQuantity();
        return quantitySome;
    }
}
