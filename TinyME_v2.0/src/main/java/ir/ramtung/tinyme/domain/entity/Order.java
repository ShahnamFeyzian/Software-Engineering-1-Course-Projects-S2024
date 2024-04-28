package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.CantQueueOrderException;
import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.domain.exception.NotEnoughExecutionException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@ToString
@Getter
public class Order {

	protected long orderId;
	protected Security security;
	protected Side side;
	protected int quantity;
	protected int minimumExecutionQuantity;
	protected int price;
	protected Broker broker;
	protected Shareholder shareholder;
	protected List<LocalDateTime> entryTimes = new ArrayList<>();

	@Builder.Default
	protected OrderStatus status = OrderStatus.NEW;

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime,
		OrderStatus status
	) {
		this.orderId = orderId;
		this.security = security;
		this.side = side;
		this.quantity = quantity;
		this.minimumExecutionQuantity = minimumExecutionQuantity;
		this.price = price;
		this.entryTimes.add(entryTime);
		this.broker = broker;
		this.shareholder = shareholder;
		this.status = status;
	}

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		List<LocalDateTime> entryTimes,
		OrderStatus status
	) {
		this.orderId = orderId;
		this.security = security;
		this.side = side;
		this.quantity = quantity;
		this.minimumExecutionQuantity = minimumExecutionQuantity;
		this.price = price;
		this.entryTimes = entryTimes;
		this.broker = broker;
		this.shareholder = shareholder;
		this.status = status;
	}

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime
	) {
		this(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTime,
			OrderStatus.NEW
		);
	}

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder
	) {
		this(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			LocalDateTime.now()
		);
	}

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder
	) {
		this(orderId, security, side, quantity, 0, price, broker, shareholder);
	}

	public Order(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime
	) {
		this(orderId, security, side, quantity, 0, price, broker, shareholder, entryTime);
	}

	public Order(Order other) {
		this(
			other.orderId,
			other.security,
			other.side,
			other.quantity,
			0,
			other.price,
			other.broker,
			other.shareholder,
			LocalDateTime.now(),
			OrderStatus.NEW
		);
	}

	public static Order createTempOrderByEnterRq(
		Security security,
		Broker broker,
		Shareholder shareholder,
		EnterOrderRq req
	) {
		return new Order(
			req.getOrderId(),
			security,
			req.getSide(),
			req.getQuantity(),
			req.getMinimumExecutionQuantity(),
			req.getPrice(),
			broker,
			shareholder,
			req.getEntryTime(),
			OrderStatus.NEW
		);
	}

	public Order snapshot() {
		return new Order(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTimes,
			OrderStatus.SNAPSHOT
		);
	}

	public Order snapshotWithQuantity(int newQuantity) {
		return new Order(
			orderId,
			security,
			side,
			newQuantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTimes,
			this.status
		);
	}

	public boolean matches(Order other) {
		if (side == Side.BUY) {
			return price >= other.price;
		}

		return price <= other.price;
	}

	public void decreaseQuantity(int amount) {
		if (amount > quantity || amount <= 0) {
			throw new IllegalArgumentException();
		}

		quantity -= amount;
		if (quantity == 0 && status == OrderStatus.QUEUED) {
			status = OrderStatus.DONE;
			security.deleteOrder(side, orderId);
		}
	}

	public void rollback(Order firstVersion) {
		this.quantity = firstVersion.quantity;
		if (status == OrderStatus.DONE) {
			security.getOrderBook().enqueue(this);
		}
	}

	public void makeQuantityZero() {
		quantity = 0;
	}

	public boolean queuesBefore(Order order) {
		if (price == order.getPrice()) {
			return entryTimes.getLast().isBefore(order.entryTimes.getLast());
		}

		if (order.getSide() == Side.BUY) {
			return price > order.getPrice();
		} else {
			return price < order.getPrice();
		}
	}

	public void queue() {
		if (this.status == OrderStatus.QUEUED) {
			throw new CantQueueOrderException();
		}

		if (side == Side.BUY && status != OrderStatus.LOADING) {
			broker.decreaseCreditBy(this.getValue());
		}
		status = OrderStatus.QUEUED;
	}

	public boolean isQuantityIncreased(int newQuantity) {
		return newQuantity > quantity;
	}

	public void updateFromTempOrder(Order tempOrder) {
		if (!this.willPriorityLostInUpdate(tempOrder) && this.side == Side.BUY) {
			broker.increaseCreditBy(this.getValue());
			broker.decreaseCreditBy(tempOrder.getValue());
		} else {
			this.status = OrderStatus.UPDATING;
		}
		this.entryTimes.add(LocalDateTime.now());
		this.quantity = tempOrder.quantity;
		this.price = tempOrder.price;
	}

	public long getValue() {
		return (long) price * quantity;
	}

	public int getTotalQuantity() {
		return quantity;
	}

	public void checkNewPeakSize(int peakSize) {
		if (peakSize != 0) {
			throw new InvalidPeakSizeException();
		}
	}

	public void checkNewMinimumExecutionQuantity(int minimumExecutionQuantity) {
		if (this.minimumExecutionQuantity != minimumExecutionQuantity) {
			throw new UpdateMinimumExecutionQuantityException();
		}
	}

	public void checkNewStopLimitPrice(int stopLimitPrice) {
		if (stopLimitPrice != 0) {
			throw new InvalidStopLimitPriceException();
		} 
	}

	public void checkExecutionQuantity(int quantitySome) {
		if (!(this.status != OrderStatus.NEW) && (quantitySome < this.minimumExecutionQuantity)) {
			throw new NotEnoughExecutionException();
		}
	}

	public void addYourselfToQueue() {
		if (this.quantity != 0) {
			this.security.getOrderBook().enqueue(this);
		}
	}

	public void delete() {
		if (side == Side.BUY) {
			broker.increaseCreditBy(getValue());
		}
	}

	public boolean willPriorityLostInUpdate(Order tempOrder) {
		return (this.quantity < tempOrder.quantity) || (this.price != tempOrder.price);
	}
}
