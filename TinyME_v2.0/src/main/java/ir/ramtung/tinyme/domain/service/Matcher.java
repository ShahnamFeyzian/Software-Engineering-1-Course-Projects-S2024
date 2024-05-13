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

	public int calcOpeningAuctionPrice(OrderBook orderBook, int lastTradePrice) {
		int minPrice = orderBook.getBuyQueue().getLast().getPrice(); // TODO: add method
		int maxPrice = orderBook.getSellQueue().getLast().getPrice(); // TODO: add method
		int maxTradableQuantity = 0;
		int openingPrice = lastTradePrice; 
		
		for (int price=minPrice; price<=maxPrice; price++) {
			int currentTradableQuantity = calcOpeningAuctionPrice(orderBook, price);
			if (currentTradableQuantity > maxTradableQuantity) {
				openingPrice = price;
				maxTradableQuantity = currentTradableQuantity;
			} 
			else if (currentTradableQuantity == maxTradableQuantity && Math.abs(price - lastTradePrice) < Math.abs(openingPrice - lastTradePrice)) {
				openingPrice = price;
			}
		}
		
		return openingPrice;
	}

	public int calcTradableQuantity(OrderBook orderBook, int openingPrice) {
		int buysQuantity = 0;
		int sellsQuantity = 0;

		for (Order order : orderBook.getBuyQueue()) {
			if (order.canTradeWithPrice(openingPrice)) {
				buysQuantity += order.getTotalQuantity();
			} 
		}

		for (Order order : orderBook.getSellQueue()) {
			if (order.canTradeWithPrice(openingPrice)) {
				sellsQuantity += order.getTotalQuantity();
			} 
		}

		return Math.min(buysQuantity, sellsQuantity);
	}

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
