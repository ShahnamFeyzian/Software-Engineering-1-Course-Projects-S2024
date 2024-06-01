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
		return orderBook.hasOrderOfType(Side.BUY) && orderBook.hasOrderOfType(Side.SELL);
	}

	public int calcOpeningAuctionPrice(OrderBook orderBook, int lastTradePrice) {
		if(!hasOrderForAuction(orderBook)) {
			return lastTradePrice;
		}

		int maxTradableQuantity = 0;
		int openingPrice = lastTradePrice; 
		int minPrice = orderBook.getLowestPriorityActiveOrder(Side.BUY).getPrice();
		int maxPrice = orderBook.getLowestPriorityActiveOrder(Side.SELL).getPrice();
		
		for (int price = minPrice; price <= maxPrice; price++) {
			int currentTradableQuantity = calcTradableQuantity(orderBook, price);
			if (shouldUpdateOpeningAuctionPrice(openingPrice, price, lastTradePrice, maxTradableQuantity, currentTradableQuantity)) {
				openingPrice = price;
				maxTradableQuantity = currentTradableQuantity;
			}
		}
		
		return openingPrice;
	}

	private boolean shouldUpdateOpeningAuctionPrice(int openingPrice, int newOpeningPrice, int lastTradePrice, int openingTradableQuantity, int newOpeningTradableQuantity) {
		return newOpeningTradableQuantity > openingTradableQuantity || 
				(newOpeningTradableQuantity == openingTradableQuantity && 
				Math.abs(newOpeningPrice - lastTradePrice) < Math.abs(openingPrice - lastTradePrice));
	}

	public int calcTradableQuantity(OrderBook orderBook, int openingPrice) {
		int buysQuantity = calcTradableQuantityInQueue(orderBook.getBuyQueue(), openingPrice);
		int sellsQuantity = calcTradableQuantityInQueue(orderBook.getSellQueue(), openingPrice);;
		return Math.min(buysQuantity, sellsQuantity);
	}

	private int calcTradableQuantityInQueue(List<Order> queue, int price) {
		return queue.stream()
			 		.filter(order -> order.canTradeWithPrice(price))
			 		.mapToInt(Order::getTotalQuantity)
			 		.sum();
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
			continuousMatchingControl.actionAfterMatching(order, orderBook);	
		} else {
			continuousMatchingControl.actionAfterFailedMatching(trades, orderBook);
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
			auctionMatchingControl.actionAfterMatching(null, orderBook);	
		} else {
			auctionMatchingControl.actionAfterFailedMatching(trades, orderBook);
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

	public MatchResult continuousExecuting(Order targetOrder, OrderBook orderBook) {
		ControlResult controlResult = startingExecution(continuousMatchingControl, targetOrder, orderBook);
		if (controlResult != ControlResult.OK) {
			return MatchResult.createFromControlResult(controlResult);
		}
		return continuousMatch(targetOrder, orderBook);
	}

	public MatchResult auctionExecuting(OrderBook orderBook, int lastTradePrice) {
		ControlResult controlResult = startingExecution(auctionMatchingControl, null, orderBook);
		if (controlResult != ControlResult.OK) {
			return MatchResult.createFromControlResult(controlResult);
		}
		int openingPrice = calcOpeningAuctionPrice(orderBook, lastTradePrice);
		return auctionMatch(orderBook, openingPrice);
	}

	private ControlResult startingExecution(MatchingControl control, Order targetOrder, OrderBook orderBook) {
		ControlResult controlResult = control.checkBeforeMatching(targetOrder, orderBook);
		if (controlResult == ControlResult.OK) {
			control.actionAtBeforeMatching(targetOrder, orderBook);
		} else {
			control.actionAtFailedBeforeMatching(targetOrder, orderBook);
		}
		return controlResult;
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
