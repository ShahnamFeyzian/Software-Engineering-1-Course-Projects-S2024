package ir.ramtung.tinyme.domain;

import static org.junit.jupiter.api.Assertions.*;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BrokerCreditTest {

	private Security security;
	private Broker broker;
	private Shareholder shareholder;
	private List<Order> orders;

	@Autowired
	Matcher matcher;

	@BeforeEach
	void setupOrderBook() {
		security = Security.builder().build();
		broker = Broker.builder().brokerId(0).credit(100_000).build();
		shareholder = Shareholder.builder().shareholderId(0).build();
		shareholder.incPosition(security, 100_000);
		orders =
			new ArrayList<>(
				Arrays.asList(
					new Order(
						1,
						security,
						Side.BUY,
						304,
						15700,
						broker,
						shareholder
					),
					new Order(
						2,
						security,
						Side.BUY,
						43,
						15500,
						broker,
						shareholder
					),
					new Order(
						3,
						security,
						Side.BUY,
						445,
						15450,
						broker,
						shareholder
					),
					new Order(
						4,
						security,
						Side.BUY,
						526,
						15450,
						broker,
						shareholder
					),
					new Order(
						5,
						security,
						Side.BUY,
						1000,
						15400,
						broker,
						shareholder
					),
					new Order(
						6,
						security,
						Side.SELL,
						350,
						15800,
						broker,
						shareholder
					),
					new Order(
						7,
						security,
						Side.SELL,
						285,
						15810,
						broker,
						shareholder
					),
					new Order(
						8,
						security,
						Side.SELL,
						800,
						15810,
						broker,
						shareholder
					),
					new Order(
						9,
						security,
						Side.SELL,
						340,
						15820,
						broker,
						shareholder
					),
					new Order(
						10,
						security,
						Side.SELL,
						65,
						15820,
						broker,
						shareholder
					)
				)
			);
		orders.forEach(order -> security.getOrderBook().enqueue(order));
	}

	@Test
	void increase_credit_by_valid_amount_credit_increased() {
		broker.increaseCreditBy(50_000);
		assertEquals(150_000, broker.getCredit());
	}

	@Test
	void decrease_credit_by_valid_amount_credit_decreased() {
		broker.decreaseCreditBy(50_000);
		assertEquals(50_000, broker.getCredit());
	}

	@Test
	void has_enough_credit_enough_credit_returns_true() {
		assertTrue(broker.hasEnoughCredit(50_000));
	}

	@Test
	void has_enough_credit_not_enough_credit_returns_false() {
		assertFalse(broker.hasEnoughCredit(150_000));
	}

	@Test
	void has_enough_credit_exact_credit_returns_true() {
		assertTrue(broker.hasEnoughCredit(100_000));
	}

	@Test
	void increase_credit_by_negative_amount_throws_exception() {
		assertThrows(
			AssertionError.class,
			() -> broker.increaseCreditBy(-50_000)
		);
	}

	@Test
	void decrease_credit_by_negative_amount_throws_exception() {
		assertThrows(
			AssertionError.class,
			() -> broker.decreaseCreditBy(-50_000)
		);
	}

	@Test
	void credit_changes_after_order_execution() {
		EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(
			1,
			security.getIsin(),
			6,
			java.time.LocalDateTime.now(),
			Side.SELL,
			350,
			15700,
			0,
			0,
			0
		);
		matcher = new Matcher();
		matcher.match(orders.get(5).snapshotWithQuantity(350));
		try {
			security.updateOrder(updateOrderRq, matcher);
			assertEquals(4872800, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_order_cancellation() {
		DeleteOrderRq deleteOrderRq = new DeleteOrderRq(
			1,
			security.getIsin(),
			Side.SELL,
			6
		);
		matcher = new Matcher();
		matcher.match(orders.get(5).snapshotWithQuantity(350));
		try {
			security.deleteOrder(deleteOrderRq);
			assertEquals(100000, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_iceberg_order_execution() {
		security = Security.builder().build();
		broker = Broker.builder().brokerId(0).credit(1_000_000).build();
		orders =
			new ArrayList<>(
				Arrays.asList(
					new Order(
						1,
						security,
						Side.BUY,
						304,
						15700,
						broker,
						shareholder
					),
					new Order(
						2,
						security,
						Side.BUY,
						43,
						15500,
						broker,
						shareholder
					),
					new IcebergOrder(
						3,
						security,
						Side.BUY,
						445,
						15450,
						broker,
						shareholder,
						100
					),
					new Order(
						4,
						security,
						Side.BUY,
						526,
						15450,
						broker,
						shareholder
					),
					new Order(
						5,
						security,
						Side.BUY,
						1000,
						15400,
						broker,
						shareholder
					)
				)
			);
		orders.forEach(order -> security.getOrderBook().enqueue(order));
		EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(
			1,
			security.getIsin(),
			3,
			java.time.LocalDateTime.now(),
			Side.BUY,
			445,
			15450,
			0,
			0,
			150
		);
		matcher = new Matcher();
		matcher.match(orders.get(2).snapshotWithQuantity(445));
		try {
			security.updateOrder(updateOrderRq, matcher);
			assertEquals(5557750, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_iceberg_order_cancellation() {
		security = Security.builder().build();
		broker = Broker.builder().brokerId(0).credit(1_000_000).build();
		orders =
			new ArrayList<>(
				Arrays.asList(
					new Order(
						1,
						security,
						Side.BUY,
						304,
						15700,
						broker,
						shareholder
					),
					new Order(
						2,
						security,
						Side.BUY,
						43,
						15500,
						broker,
						shareholder
					),
					new IcebergOrder(
						3,
						security,
						Side.BUY,
						445,
						15450,
						broker,
						shareholder,
						100
					),
					new Order(
						4,
						security,
						Side.BUY,
						526,
						15450,
						broker,
						shareholder
					),
					new Order(
						5,
						security,
						Side.BUY,
						1000,
						15400,
						broker,
						shareholder
					)
				)
			);
		orders.forEach(order -> security.getOrderBook().enqueue(order));
		DeleteOrderRq deleteOrderRq = new DeleteOrderRq(
			1,
			security.getIsin(),
			Side.BUY,
			3
		);
		matcher = new Matcher();
		matcher.match(orders.get(2).snapshotWithQuantity(445));
		try {
			security.deleteOrder(deleteOrderRq);
			assertEquals(7875250, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_order_update() {
		EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(
			1,
			security.getIsin(),
			6,
			java.time.LocalDateTime.now(),
			Side.SELL,
			350,
			15700,
			0,
			0,
			0
		);
		matcher = new Matcher();
		matcher.match(orders.get(5).snapshotWithQuantity(350));
		try {
			security.updateOrder(updateOrderRq, matcher);
			assertEquals(4872800, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_order_update_with_different_price() {
		EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(
			1,
			security.getIsin(),
			6,
			java.time.LocalDateTime.now(),
			Side.SELL,
			350,
			15750,
			0,
			0,
			0
		);
		matcher = new Matcher();
		matcher.match(orders.get(5).snapshotWithQuantity(350));
		try {
			security.updateOrder(updateOrderRq, matcher);
			assertEquals(100000, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}

	@Test
	void credit_changes_after_order_update_with_different_quantity() {
		EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(
			1,
			security.getIsin(),
			6,
			java.time.LocalDateTime.now(),
			Side.SELL,
			300,
			15700,
			0,
			0,
			0
		);
		matcher = new Matcher();
		matcher.match(orders.get(5).snapshotWithQuantity(300));
		try {
			security.updateOrder(updateOrderRq, matcher);
			assertEquals(4810000, broker.getCredit());
		} catch (InvalidRequestException e) {
			fail("Exception thrown");
		}
	}
}
