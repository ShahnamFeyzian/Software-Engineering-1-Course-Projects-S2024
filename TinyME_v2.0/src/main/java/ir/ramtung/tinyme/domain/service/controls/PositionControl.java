package ir.ramtung.tinyme.domain.service.controls;


import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.domain.entity.Shareholder;
import ir.ramtung.tinyme.domain.entity.Trade;

@Service
public class PositionControl {
    public ControlResult checkPositionForOrder(Order order, OrderBook orderBook) {
        if (order.isBuy()) {
			return ControlResult.OK;
		}

		Shareholder shareholder = order.getShareholder();
		Security security = order.getSecurity();
        int salesAmount = order.getQuantity();
		int queuedPositionAmount = orderBook.totalSellQuantityByShareholder(shareholder);
		int totalNeededPosition = salesAmount + queuedPositionAmount;

		if (shareholder.hasEnoughPositionsOn(security, totalNeededPosition)) {
			return ControlResult.OK;
		}
        else {
            return ControlResult.NOT_ENOUGH_POSITION;
        }
    }

	public void updatePositionsAtTrade(Trade trade) {
		updateBuyerPositionAtTrade(trade);
		updateSellerPositionAtTrade(trade);
	}

	public void updatePositionsAtRollbackTrade(Trade trade) {
		updateBuyerPositionAtRollbackTrade(trade);
		updateSellerPositionAtRollbackTrade(trade);
	}

	private void updateBuyerPositionAtTrade(Trade trade) {
		Order buyOrder = trade.getBuy();
		Shareholder buyerShareholder = buyOrder.getShareholder();
		int tradeQuantity = trade.getQuantity();
		Security security = trade.getSecurity();

		buyerShareholder.incPosition(security, tradeQuantity);
	}

	private void updateSellerPositionAtTrade(Trade trade) {
		Order sellOrder = trade.getSell();
		Shareholder sellerShareholder = sellOrder.getShareholder();
		int tradeQuantity = trade.getQuantity();
		Security security = trade.getSecurity();

		sellerShareholder.decPosition(security, tradeQuantity);
	}

	private void updateBuyerPositionAtRollbackTrade(Trade trade) {
		Order buyOrder = trade.getBuy();
		Shareholder buyerShareholder = buyOrder.getShareholder();
		Security security = buyOrder.getSecurity();
		int tradeQuantity = trade.getQuantity();

		buyerShareholder.decPosition(security, tradeQuantity);
	}

	private void updateSellerPositionAtRollbackTrade(Trade trade) {
		Order sellOrder = trade.getSell();
		Shareholder sellerShareholder = sellOrder.getShareholder();
		Security security = sellOrder.getSecurity();
		int tradeQuantity = trade.getQuantity();
		
		sellerShareholder.incPosition(security, tradeQuantity);
	}
}
