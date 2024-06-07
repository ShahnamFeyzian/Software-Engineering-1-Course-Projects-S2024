package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.entity.stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.stats.SecurityStats;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.controls.AuctionMatchingControl;
import ir.ramtung.tinyme.domain.service.controls.ContinuousMatchingControl;
import ir.ramtung.tinyme.domain.service.controls.CreditControl;
import ir.ramtung.tinyme.domain.service.controls.PositionControl;
import ir.ramtung.tinyme.domain.service.controls.QuantityControl;
import ir.ramtung.tinyme.domain.service.security_state.AuctionBehave;
import ir.ramtung.tinyme.domain.service.security_state.ContinuousBehave;
import ir.ramtung.tinyme.domain.service.security_state.SecurityBehave;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import java.util.LinkedList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Security {

	private String isin;

	@Builder.Default
	private int tickSize = 1;

	@Builder.Default
	private int lotSize = 1;

	@Builder.Default
	private OrderBook orderBook = new OrderBook();

	private int lastTradePrice;

	//FIXME: this is turning to something really ugly
	private static PositionControl positionControl = new PositionControl();
	private static CreditControl creditControl = new CreditControl();
	private static QuantityControl quantityControl = new QuantityControl();
	private static Matcher matcher = new Matcher(new ContinuousMatchingControl(positionControl, creditControl, quantityControl), new AuctionMatchingControl(positionControl, creditControl, quantityControl));
	private static ContinuousBehave continuousBehave = new ContinuousBehave(positionControl, creditControl, matcher);
	private static AuctionBehave auctionBehave = new AuctionBehave(positionControl, creditControl, matcher);

	@Builder.Default
	private SecurityBehave currentBehave = continuousBehave;

	@Builder.Default
	private SecurityState state = SecurityState.CONTINUOUS;

	public SecurityResponse addNewOrder(Order newOrder) {
		List<SecurityStats> stats = currentBehave.addNewOrder(newOrder, orderBook, lastTradePrice);
		updateLastTradePrice(stats);
		stats.addAll(currentBehave.activateStopLimitOrders(orderBook, lastTradePrice));
		updateLastTradePrice(stats);
		return new SecurityResponse(stats);
	}

	public SecurityResponse updateOrder(Order tempOrder) {
		Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
		List<SecurityStats> stats = currentBehave.updateOrder(tempOrder, mainOrder, orderBook, lastTradePrice);
		updateLastTradePrice(stats);
		stats.addAll(currentBehave.activateStopLimitOrders(orderBook, lastTradePrice));
		updateLastTradePrice(stats);
		return new SecurityResponse(stats);
	}

	public SecurityResponse deleteOrder(Side side, long orderId) {
		Order order = findByOrderId(side, orderId);
		List<SecurityStats> stats = currentBehave.deleteOrder(order, orderBook, lastTradePrice);
		return new SecurityResponse(stats);
	}

	public SecurityResponse changeMatchingState(SecurityState newState) {
		//FIXME: maybe refactor!!!
		List<SecurityStats> stats = currentBehave.changeMatchingState(orderBook, lastTradePrice, newState);
		updateLastTradePrice(stats);
		currentBehave = (newState == SecurityState.AUCTION) ? auctionBehave : continuousBehave;
		if (this.state == SecurityState.AUCTION)
			stats.addAll(currentBehave.activateStopLimitOrders(orderBook, lastTradePrice));
		updateLastTradePrice(stats);
		this.state = newState;
		return new SecurityResponse(stats);
	}

	private void updateLastTradePrice(List<SecurityStats> stats) {
		for (SecurityStats stat : stats.reversed()) {
			if (stat instanceof ExecuteStats exeStat) {
				lastTradePrice = exeStat.getTrades().getLast().getPrice();
				return;
			}
		}
	}

	public List<String> checkEnterOrderRq(EnterOrderRq order) {
		List<String> errors = new LinkedList<>();
		
		if (order.getQuantity() % lotSize != 0) {
			errors.add(Message.QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE);
		}

		if (order.getPrice() % tickSize != 0) {
			errors.add(Message.PRICE_NOT_MULTIPLE_OF_TICK_SIZE);
		}

		if(this.state == SecurityState.AUCTION && order.getMinimumExecutionQuantity() != 0) {
			errors.add(Message.MINIMUM_EXECUTION_IN_AUCTION_STATE);
		}

		if(this.state == SecurityState.AUCTION && order.getStopPrice() != 0) {
			errors.add(Message.STOP_PRICE_IN_AUCTION_STATE);
		}

		return errors;
	}

	public Order findByOrderId(Side side, long orderId) {
		return orderBook.findByOrderId(side, orderId);
	}

	public boolean isThereOrderWithId(Side side, long orderId) {
		return orderBook.isThereOrderWithId(side, orderId);
	}

	public boolean isStopLimitOrder(Side side, long orderId) {
		Order order = orderBook.findByOrderId(side, orderId);
		return (order instanceof StopLimitOrder);
	}
}
