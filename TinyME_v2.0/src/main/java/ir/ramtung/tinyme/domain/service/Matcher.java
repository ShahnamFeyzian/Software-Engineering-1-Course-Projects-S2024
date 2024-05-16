package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotEnoughExecutionException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Matcher {

	private boolean hesOrderForAuction(OrderBook orderBook) {
		return orderBook.hasOrderOfType(Side.BUY) &&
				orderBook.hasOrderOfType(Side.SELL);
	}

	public int calcOpeningAuctionPrice(OrderBook orderBook, int lastTradePrice) {
		if(!hesOrderForAuction(orderBook))
			return 0;

		int minPrice = orderBook.getLowestPriorityActiveOrder(Side.BUY).getPrice();
		int maxPrice = orderBook.getLowestPriorityActiveOrder(Side.SELL).getPrice();

		int maxTradableQuantity = 0;
		int openingPrice = lastTradePrice; 
		
		for (int price=minPrice; price<=maxPrice; price++) {
			int currentTradableQuantity = calcTradableQuantity(orderBook, price);
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
				Order matchingOrder = orderBook.findOrderToMatchWith(newOrder);
				if (newOrder.getSide() == Side.SELL) {
					trades.add(createTrade(newOrder, matchingOrder, matchingOrder.getPrice()));
				} else {
					trades.add(createTrade(matchingOrder, newOrder, matchingOrder.getPrice()));
				}
			}
			return trades;
		} catch (NotFoundException exp) {
			return trades;
		} catch (NotEnoughCreditException exp) {
			rollbackTrades(trades);
			throw exp;
		}
	}

	private List<Trade> auctionMatch(OrderBook orderBook, int openingPrice) {
		List<Trade> trades = new ArrayList<>();
		if(!hesOrderForAuction(orderBook)) {
			return trades;
		}
		Order sellOrder = orderBook.getHighestPriorityActiveOrder(Side.SELL);
		Order buyOrder = orderBook.getHighestPriorityActiveOrder(Side.BUY);
		while(sellOrder.canTradeWithPrice(openingPrice) && buyOrder.canTradeWithPrice(openingPrice)) {
			trades.add(createTrade(sellOrder, buyOrder, openingPrice));
			// FIXME: maybe more clear?
			if(!hesOrderForAuction(orderBook))
				break;
			sellOrder = orderBook.getHighestPriorityActiveOrder(Side.SELL);
			buyOrder = orderBook.getHighestPriorityActiveOrder(Side.BUY);
		}
		return trades;
	}

	private Trade createTrade(Order sellOrder, Order buyOrder, int price) {
		Trade trade = new Trade(sellOrder, buyOrder, price);
		trade.confirm();
		return trade;
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

	public List<Trade> auctionExecuting(OrderBook orderBook, int lastTradePrice) {
		int openingPrice = calcOpeningAuctionPrice(orderBook, lastTradePrice);
		return auctionMatch(orderBook, openingPrice);
	}

	private int sumOfExecutionQuantity(List<Trade> trades) {
		int quantitySum = 0;
		for (Trade trade : trades) quantitySum += trade.getQuantity();
		return quantitySum;
	}
}
