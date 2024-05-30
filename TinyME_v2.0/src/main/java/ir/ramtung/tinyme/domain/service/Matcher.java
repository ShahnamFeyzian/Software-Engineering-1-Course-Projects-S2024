package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.controls.AuctionMatchingControl;
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
	private MatchingControl auctionMatchingControl;

	public Matcher(ContinuousMatchingControl continuousMatchingControl, AuctionMatchingControl auctionMatchingControl) {
		this.continuousMatchingControl = continuousMatchingControl;
		this.auctionMatchingControl = auctionMatchingControl;
	}

	private boolean hasOrderForAuction(OrderBook orderBook) {
		return orderBook.hasOrderOfType(Side.BUY) &&
				orderBook.hasOrderOfType(Side.SELL);
	}

	public int calcOpeningAuctionPrice(OrderBook orderBook, int lastTradePrice) {
		if(!hasOrderForAuction(orderBook))
			return lastTradePrice;

			int maxTradableQuantity = 0;
			int openingPrice = lastTradePrice; 
			int minPrice = orderBook.getLowestPriorityActiveOrder(Side.BUY).getPrice();
			int maxPrice = orderBook.getLowestPriorityActiveOrder(Side.SELL).getPrice();
		
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

		while ((matchingOrder = getMatchingOrder(order, orderBook)) != null) {
			Trade trade = createTradeForContinuousMatching(order, matchingOrder);
			controlResult = continuousMatchingControl.checkBeforeMatch(trade);
			if (controlResult == ControlResult.OK) {
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

	private MatchResult auctionMatch(OrderBook orderBook, int openingPrice) {
		ControlResult controlResult;
		List<Trade> trades = new ArrayList<>();
		Trade currentTrade;
		while((currentTrade = createTradeForAuctionMatching(orderBook, openingPrice)) != null) {
			controlResult = auctionMatchingControl.checkBeforeMatch(currentTrade);
			if (controlResult == ControlResult.OK) {
				auctionMatchingControl.actionAtMatch(currentTrade, orderBook);
				trades.add(currentTrade);
			} else {
				auctionMatchingControl.actionAtFailedBeforeMatch(trades, orderBook);
				return MatchResult.createFromControlResult(controlResult);
			}
		}
		
		controlResult = auctionMatchingControl.checkAfterMatching(null, trades);
		if (controlResult == ControlResult.OK) {
			auctionMatchingControl.actionAtAfterMatching(null, orderBook);	
		} else {
			auctionMatchingControl.actionAtfailedAfterMatching(trades, orderBook);
			return MatchResult.createFromControlResult(controlResult);
		}

		return MatchResult.executed(null, trades);
	}

	private Trade createTradeForAuctionMatching(OrderBook orderBook, int openingPrice) {
		if (!hasOrderForAuction(orderBook)){
			return null;
		}

		Order sellOrder = orderBook.getHighestPriorityActiveOrder(Side.SELL);
		Order buyOrder = orderBook.getHighestPriorityActiveOrder(Side.BUY);
		if (sellOrder.canTradeWithPrice(openingPrice) && buyOrder.canTradeWithPrice(openingPrice)) {
			return new Trade(sellOrder, buyOrder, openingPrice);
		} else {
			return null;
		}
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

	public MatchResult continuousExecuting(Order order, OrderBook orderBook) {
		ControlResult controlResult = continuousMatchingControl.checkBeforeMatching(order, orderBook);
		if (controlResult == ControlResult.OK) {
			continuousMatchingControl.actionAtBeforeMatching(order, orderBook);
		} else {
			continuousMatchingControl.actionAtFailedBeforeMatching(order, orderBook);
			return MatchResult.createFromControlResult(controlResult);
		}

		return continuousMatch(order, orderBook);
	}

	public MatchResult auctionExecuting(OrderBook orderBook, int lastTradePrice) {
		ControlResult controlResult = auctionMatchingControl.checkBeforeMatching(null, orderBook);
		if (controlResult == ControlResult.OK) {
			auctionMatchingControl.actionAtBeforeMatching(null, orderBook);
		} else {
			auctionMatchingControl.actionAtFailedBeforeMatching(null, orderBook);
			return MatchResult.createFromControlResult(controlResult);
		}

		int openingPrice = calcOpeningAuctionPrice(orderBook, lastTradePrice);
		return auctionMatch(orderBook, openingPrice);
	}

	private Order getMatchingOrder(Order targetOrder, OrderBook orderBook) {
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
}
