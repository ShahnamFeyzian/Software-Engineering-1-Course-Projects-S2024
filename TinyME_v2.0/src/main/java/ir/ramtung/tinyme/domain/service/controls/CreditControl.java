package ir.ramtung.tinyme.domain.service.controls;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Trade;
import org.springframework.stereotype.Service;

@Service
public class CreditControl {

	public ControlResult checkCreditForTrade(Trade trade) {
		if (trade.isBuyQueued()) {
			return ControlResult.OK;
		}

		Order targetOrder = trade.getBuy();
		long value = trade.getTradedValue();
		Broker broker = targetOrder.getBroker();

		if (broker.hasEnoughCredit(value)) {
			return ControlResult.OK;
		} else {
			return ControlResult.NOT_ENOUGH_CREDIT;
		}
	}

	public ControlResult checkCreditForBeingQueued(Order order) {
		if (order.isSell()) {
			return ControlResult.OK;
		}

		long value = order.getValue();
		Broker broker = order.getBroker();

		if (broker.hasEnoughCredit(value)) {
			return ControlResult.OK;
		} else {
			return ControlResult.NOT_ENOUGH_CREDIT;
		}
	}

	public void updateCreditsAtTrade(Trade trade) {
		updateBuyerCreditAtTrade(trade);
		updateSellerCreditAtTrade(trade);
	}

	public void updateCreditsAtRollbackTrade(Trade trade) {
		updateBuyerCreditAtRollbackTrade(trade);
		updateSellerCreditAtRollbackTrade(trade);
	}

	public void updateCreditForBeingQueued(Order targetOrder) {
		if (targetOrder.isBuy()) {
			Broker buyerBroker = targetOrder.getBroker();
			long remainderValue = targetOrder.getValue();
			buyerBroker.decreaseCreditBy(remainderValue);
		}
	}

	public void updateCreditAtDelete(Order order) {
		if (order.isBuy()) {
			Broker buyerBroker = order.getBroker();
			long orderValue = order.getValue();
			buyerBroker.increaseCreditBy(orderValue);
		}
	}

	private void updateBuyerCreditAtTrade(Trade trade) {
		Order buyOrder = trade.getBuy();
		Broker buyerBroker = buyOrder.getBroker();
		long tradeValue = trade.getTradedValue();

		if (!buyOrder.isQueued()) {
			buyerBroker.decreaseCreditBy(tradeValue);
		} else {
			if (trade.getPrice() < buyOrder.getPrice()) {
				long backCredit = (long) ((buyOrder.getPrice() - trade.getPrice()) * trade.getQuantity());
				buyOrder.getBroker().increaseCreditBy(backCredit);
			}
		} 
	}

	private void updateSellerCreditAtTrade(Trade trade) {
		Order sellOrder = trade.getSell();
		Broker sellerBroker = sellOrder.getBroker();
		long tradeValue = trade.getTradedValue();

		sellerBroker.increaseCreditBy(tradeValue);
	}

	private void updateBuyerCreditAtRollbackTrade(Trade trade) {
		Order buyOrder = trade.getBuy();
		Broker buyerBroker = buyOrder.getBroker();
		long tradeValue = trade.getTradedValue();

		if (!buyOrder.isQueued()) {
			buyerBroker.increaseCreditBy(tradeValue);
		}
	}

	private void updateSellerCreditAtRollbackTrade(Trade trade) {
		Order sellOrder = trade.getSell();
		Broker sellerBroker = sellOrder.getBroker();
		long tradeValue = trade.getTradedValue();

		sellerBroker.decreaseCreditBy(tradeValue);
	}
}
