package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.time.LocalDateTime;

import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StopLimitOrder extends Order {

	private int stopPrice;

	public StopLimitOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		int stopPrice
	) {
		super(orderId, security, side, quantity, price, broker, shareholder);
		this.stopPrice = stopPrice;
	}

	public StopLimitOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime,
		int stopPrice,
		OrderStatus status
	) {
		super(orderId, security, side, quantity, 0, price, broker, shareholder, entryTime, status);
		this.stopPrice = stopPrice;
	}

	public static StopLimitOrder createTempOrderByEnterRq(
		Security security,
		Broker broker,
		Shareholder shareholder,
		EnterOrderRq req
	) {
		return new StopLimitOrder(
			req.getOrderId(),
			security,
			req.getSide(),
			req.getQuantity(),
			req.getPrice(),
			broker,
			shareholder,
			req.getEntryTime(),
			req.getStopPrice(),
			OrderStatus.NEW
		);
	}

	@Override
	public StopLimitOrder snapshot() {
		return new StopLimitOrder(
			orderId,
			security,
			side,
			quantity,
			price,
			broker,
			shareholder,
			entryTime,
			stopPrice,
			OrderStatus.SNAPSHOT
		);
	}

	public boolean isSatisfied(int lastTradePrice) {
		if (side == Side.BUY && stopPrice <= lastTradePrice) return true; else if (
			side == Side.SELL && stopPrice >= lastTradePrice
		) return true;

		return false;
	}

	@Override
	public boolean queuesBefore(Order order) {
		StopLimitOrder sloOrder = (StopLimitOrder) order;
		if (this.side == Side.BUY) return stopPrice < sloOrder.getStopPrice(); else return (
			stopPrice > sloOrder.getStopPrice()
		);
	}

	@Override
	public void checkNewStopLimitPrice(int stopLimitPrice) {
		if (stopLimitPrice == 0) throw new InvalidStopLimitPriceException();
	}

	@Override
	public void queue() {
		if(side == Side.BUY)
			broker.decreaseCreditBy(getValue());
	}

	@Override
	public void updateFromTempOrder(Order tempOrder) {
		super.updateFromTempOrder(tempOrder);
		StopLimitOrder tempSlo = (StopLimitOrder) tempOrder;
		this.stopPrice = tempSlo.stopPrice;
	}

	@Override
	public boolean willPriorityLostInUpdate(Order tempOrder) {
		if (super.willPriorityLostInUpdate(tempOrder)) {
			return true;
		}
		StopLimitOrder tempSlo = (StopLimitOrder) tempOrder;
		return this.stopPrice != tempSlo.stopPrice;
	}
}
