package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.service.controls.ContinuousMatchingControl;
import ir.ramtung.tinyme.domain.service.controls.ControlResult;
import ir.ramtung.tinyme.domain.service.controls.MatchingControl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Matcher {
	private MatchingControl continuousMatchingControl;

	public Matcher(ContinuousMatchingControl continuousMatchingControl) {
		this.continuousMatchingControl = continuousMatchingControl;
	}

	private boolean hasOrderForAuction(OrderBook orderBook) {
		return orderBook.hasOrderOfType(Side.BUY) &&
				orderBook.hasOrderOfType(Side.SELL);
	}

	public int calcOpeningAuctionPrice(OrderBook orderBook, int lastTradePrice) {
		if(!hasOrderForAuction(orderBook))
			return lastTradePrice;

		int minPrice = orderBook.getLowestPriorityActiveOrder(Side.BUY).getPrice();
		int maxPrice = orderBook.getLowestPriorityActiveOrder(Side.SELL).getPrice();

		int maxTradableQuantity = 0;
		int openingPrice = lastTradePrice; 
		
		for (int price = minPrice; price <= maxPrice; price++) {
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

	public MatchResult continuousMatch(Order order, OrderBook orderBook) {
		List<Trade> trades = new LinkedList<>();
		ControlResult controlResult;
		Order matchingOrder;

		while ((matchingOrder = getMatchingOrderInContinuousMatching(order, orderBook)) != null) {
			controlResult = continuousMatchingControl.checkBeforeMatch(order, matchingOrder);
			if (controlResult == ControlResult.OK) {
				Trade trade = createTradeForContinuousMatching(order, matchingOrder);
				continuousMatchingControl.actionAtMatch(trade, orderBook);
				trades.add(trade);
			} else {
				continuousMatchingControl.actionAtFailedBeforeMatch(trades, orderBook);
				return MatchResult.createFromControlResult(controlResult);
			}
		}

		controlResult = continuousMatchingControl.checkAfterMatching(order, trades);
		if (controlResult == ControlResult.OK) {
			continuousMatchingControl.actionAtAfterMatching(order, orderBook);	
		} else {
			continuousMatchingControl.actionAtfailedAfterMatching(trades, orderBook);
			return MatchResult.createFromControlResult(controlResult);
		}

		return MatchResult.executed(order, trades);
	}

	private List<Trade> auctionMatch(OrderBook orderBook, int openingPrice) {
		List<Trade> trades = new ArrayList<>();
		if(!hasOrderForAuction(orderBook)) {
			return trades;
		}
		Order sellOrder = orderBook.getHighestPriorityActiveOrder(Side.SELL);
		Order buyOrder = orderBook.getHighestPriorityActiveOrder(Side.BUY);
		while(sellOrder.canTradeWithPrice(openingPrice) && buyOrder.canTradeWithPrice(openingPrice)) {
			trades.add(createTrade(sellOrder, buyOrder, openingPrice));
			// FIXME: maybe more clear?
			if(!hasOrderForAuction(orderBook))
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

	// TODO: adding controls was successful, now clean this shit
	public MatchResult continuousExecuting(Order order, OrderBook orderBook) {
		ControlResult controlResult = continuousMatchingControl.checkBeforeMatching(order, orderBook);
		if (controlResult == ControlResult.OK) {
			continuousMatchingControl.actionAtBeforeMatching(order, orderBook);
		} else {
			continuousMatchingControl.actionAtFailedBeforMatching(order, orderBook);
			return MatchResult.createFromControlResult(controlResult);
		}

		return continuousMatch(order, orderBook);
	}

	private Order getMatchingOrderInContinuousMatching(Order targetOrder, OrderBook orderBook) {
		if (targetOrder.getQuantity() == 0) {
			return null;
		}

		return orderBook.findOrderToMatchWith(targetOrder);
	}

	private Trade createTradeForContinuousMatching(Order targetOrder, Order matchingOrder) {
        if (targetOrder.isSell()) {
			return new Trade(targetOrder, matchingOrder, matchingOrder.getPrice());
		} else {
			return new Trade(matchingOrder, targetOrder, matchingOrder.getPrice());
		}
    }

	public List<Trade> auctionExecuting(OrderBook orderBook, int lastTradePrice) {
		int openingPrice = calcOpeningAuctionPrice(orderBook, lastTradePrice);
		return auctionMatch(orderBook, openingPrice);
	}
}
