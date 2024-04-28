package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.InvalidIcebergPeakSizeException;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.time.LocalDateTime;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcebergOrder extends Order {

	int peakSize;
	int displayedQuantity;

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime,
		int peakSize,
		int displayedQuantity,
		OrderStatus status
	) {
		super(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTime,
			status
		);
		this.peakSize = peakSize;
		this.displayedQuantity = displayedQuantity;
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		List<LocalDateTime> entryTimes,
		int peakSize,
		int displayedQuantity,
		OrderStatus status
	) {
		super(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTimes,
			status
		);
		this.peakSize = peakSize;
		this.displayedQuantity = displayedQuantity;
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime,
		int peakSize,
		OrderStatus status
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
			peakSize,
			Math.min(peakSize, quantity),
			status
		);
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		List<LocalDateTime> entryTimes,
		int peakSize,
		OrderStatus status
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
			entryTimes.getFirst(),
			peakSize,
			Math.min(peakSize, quantity),
			status
		);
		this.entryTimes = entryTimes;
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		LocalDateTime entryTime,
		int peakSize
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
			peakSize,
			OrderStatus.NEW
		);
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int minimumExecutionQuantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		int peakSize
	) {
		super(orderId, security, side, quantity, minimumExecutionQuantity, price, broker, shareholder);
		this.peakSize = peakSize;
		this.displayedQuantity = Math.min(peakSize, quantity);
	}

	public IcebergOrder(
		long orderId,
		Security security,
		Side side,
		int quantity,
		int price,
		Broker broker,
		Shareholder shareholder,
		int peakSize
	) {
		this(orderId, security, side, quantity, 0, price, broker, shareholder, peakSize);
	}

	public static IcebergOrder createTempOrderByEnterRq(
		Security security,
		Broker broker,
		Shareholder shareholder,
		EnterOrderRq req
	) {
		return new IcebergOrder(
			req.getOrderId(),
			security,
			req.getSide(),
			req.getQuantity(),
			req.getMinimumExecutionQuantity(),
			req.getPrice(),
			broker,
			shareholder,
			req.getEntryTime(),
			req.getPeakSize(),
			OrderStatus.NEW
		);
	}

	@Override
	public Order snapshot() {
		return new IcebergOrder(
			orderId,
			security,
			side,
			quantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTimes,
			peakSize,
			OrderStatus.SNAPSHOT
		);
	}

	@Override
	public Order snapshotWithQuantity(int newQuantity) {
		return new IcebergOrder(
			orderId,
			security,
			side,
			newQuantity,
			minimumExecutionQuantity,
			price,
			broker,
			shareholder,
			entryTimes,
			peakSize,
			this.status
		);
	}

	@Override
	public int getQuantity() {
		if (status != OrderStatus.QUEUED) {
			return super.getQuantity();
		}
		
		return displayedQuantity;
	}

	@Override
	public void queue() {
		super.queue();
		this.replenish();
	}

	@Override
	public void decreaseQuantity(int amount) {
		if (status != OrderStatus.QUEUED) {
			super.decreaseQuantity(amount);
			return;
		}
		if (amount > displayedQuantity || amount <= 0) {
			throw new IllegalArgumentException();
		}

		quantity -= amount;
		displayedQuantity -= amount;
		checkEmptyDisplayQuantity();
	}

	private void checkEmptyDisplayQuantity() {
		if (displayedQuantity == 0) {
			status = OrderStatus.DONE;
			security.deleteOrder(side, orderId);
			if (quantity != 0) {
				security.getOrderBook().enqueue(this);
			}
		}
	}

	@Override
	public void rollback(Order firstVersion) {
		IcebergOrder fVersion = (IcebergOrder) firstVersion;
		this.displayedQuantity = fVersion.displayedQuantity;
		super.rollback(firstVersion);
	}

	public void replenish() {
		displayedQuantity = Math.min(quantity, peakSize);
	}

	@Override
	public void updateFromTempOrder(Order tempOrder) {
		super.updateFromTempOrder(tempOrder);
		IcebergOrder tempIcebergOrder = (IcebergOrder) tempOrder;

		if (peakSize < tempIcebergOrder.peakSize || displayedQuantity > quantity) {
			displayedQuantity = Math.min(quantity, tempIcebergOrder.peakSize);
		}

		peakSize = tempIcebergOrder.peakSize;
	}

	@Override
	public void checkNewPeakSize(int peakSize) {
		if (peakSize == 0) {
			throw new InvalidIcebergPeakSizeException();
		}
	}

	@Override
	public boolean willPriorityLostInUpdate(Order tempOrder) {
		if (super.willPriorityLostInUpdate(tempOrder)) {
			return true;
		}

		IcebergOrder tempIcebergOrder = (IcebergOrder) tempOrder;
		return this.peakSize < tempIcebergOrder.peakSize;
	}
}
