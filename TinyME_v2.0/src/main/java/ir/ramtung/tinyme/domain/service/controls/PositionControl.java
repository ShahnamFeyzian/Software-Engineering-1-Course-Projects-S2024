package ir.ramtung.tinyme.domain.service.controls;


import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.domain.entity.Shareholder;

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
}
