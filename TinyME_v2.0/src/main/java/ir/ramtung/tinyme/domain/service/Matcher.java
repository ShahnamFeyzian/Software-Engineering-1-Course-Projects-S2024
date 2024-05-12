package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotEnoughExecutionException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Matcher {

	public List<Trade> continuesMatch(Order newOrder) {
		OrderBook orderBook = newOrder.getSecurity().getOrderBook();
		LinkedList<Trade> trades = new LinkedList<>();
		try {
			while (hasOrderToMatch(newOrder, orderBook)) {
				tryToTrade(newOrder, orderBook, trades);
			}
			return trades;
		} catch (NotFoundException exp) {
			return trades;
		} catch (NotEnoughCreditException exp) {
			rollbackTrades(trades);
			throw exp;
		}
	}

    private void tryToTrade(Order newOrder, OrderBook orderBook, LinkedList<Trade> trades) {
        Order matchingOrder = orderBook.findOrderToMatchWith(newOrder);
        Trade trade = new Trade(newOrder, matchingOrder);
        trade.confirm();
        trades.add(trade);
    }

	private boolean hasOrderToMatch(Order newOrder, OrderBook orderBook) {
		return (orderBook.hasOrderOfType(newOrder.getSide().opposite())) && (newOrder.getQuantity() > 0);
	}

	private void rollbackTrades(List<Trade> trades) {
		trades.reversed().forEach(Trade::rollback);
	}

	public MatchResult continuesExecuting(Order order) {
		List<Trade> trades = new LinkedList<>();
		
		try {
			trades = continuesMatch(order);
			order.checkExecutionQuantity(sumOfExecutionQuantity(trades));
			order.addYourselfToQueue();
			return MatchResult.executed(order, trades);
		} catch (NotEnoughCreditException exp) {
			rollbackTrades(trades);
			return MatchResult.notEnoughCredit();
		} catch (NotEnoughExecutionException exp) {
			rollbackTrades(trades);
			return MatchResult.notEnoughExecution();
		}
	}

	private int sumOfExecutionQuantity(List<Trade> trades) {
		int quantitySum = 0;
		for (Trade trade : trades) quantitySum += trade.getQuantity();
		return quantitySum;
	}
}
