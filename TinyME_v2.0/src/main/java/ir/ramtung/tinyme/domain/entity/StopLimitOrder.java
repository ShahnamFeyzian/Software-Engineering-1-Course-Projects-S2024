package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.time.LocalDateTime;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StopLimitOrder extends Order {

	private int stopPrice;
	private long requestId;

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
		long requestId,
		OrderStatus status
	) {
		super(orderId, security, side, quantity, 0, price, broker, shareholder, entryTime, status);
		this.stopPrice = stopPrice;
		this.requestId =  requestId;
	}

	public StopLimitOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		int stopPrice,
		long requestId
	) {
		super(orderId, security, side, quantity, 0, price, broker, shareholder, LocalDateTime.now(), OrderStatus.NEW);
		this.stopPrice = stopPrice;
		this.requestId =  requestId;
	}

	public StopLimitOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		List<LocalDateTime> entryTimes,
		int stopPrice,
		OrderStatus status
	) {
		super(orderId, security, side, quantity, 0, price, broker, shareholder, entryTimes, status);
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
			req.getRequestId(),
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
			entryTimes,
			stopPrice,
			OrderStatus.SNAPSHOT
		);
	}

	@Override
	public boolean queuesBefore(Order order) {
		StopLimitOrder slo = (StopLimitOrder) order;
		if (stopPrice == slo.stopPrice) {
			return entryTimes.getLast().isBefore(slo.entryTimes.getLast());
		}
		if (this.side == Side.BUY) {
			return stopPrice < slo.getStopPrice();
		} else {
			return (stopPrice > slo.getStopPrice());
		}
	}

	@Override
	public void checkNewStopLimitPrice(int stopLimitPrice) {
		if (stopLimitPrice == 0) {
			throw new InvalidStopLimitPriceException();
		}
	}

	@Override
	public void updateFromTempOrder(Order tempOrder) {
		super.updateFromTempOrder(tempOrder);
		StopLimitOrder tempSlo = (StopLimitOrder) tempOrder;
		this.stopPrice = tempSlo.stopPrice;
		this.requestId = tempSlo.requestId;
	}

	@Override
	public boolean willPriorityLostInUpdate(Order tempOrder) {
		if (super.willPriorityLostInUpdate(tempOrder)) {
			return true;
		}

		StopLimitOrder tempSlo = (StopLimitOrder) tempOrder;
		return this.stopPrice != tempSlo.stopPrice;
	}

	public boolean isSatisfied(int lastTradePrice) {
		if (side == Side.BUY && stopPrice <= lastTradePrice) {
			return true;
		}

		if (side == Side.SELL && stopPrice >= lastTradePrice) {
			return true;
		}

		return false;
	}
}
