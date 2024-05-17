package ir.ramtung.tinyme.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
@DirtiesContext
public class MatcherTest {

	private Security security;
	private Broker broker;
	private Shareholder shareholder;
	private OrderBook orderBook;
	private List<Order> orders;

	@Autowired
	private Matcher matcher;

	@BeforeEach
	void setupOrderBook() {
		security = Security.builder().build();
		broker = Broker.builder().credit(100_000_000L).build();
		shareholder = Shareholder.builder().build();
		shareholder.incPosition(security, 100_000);
		orderBook = security.getOrderBook();
		orders =
			Arrays.asList(
				new Order(1, security, Side.BUY, 304, 15700, broker, shareholder),
				new Order(2, security, Side.BUY, 43, 15500, broker, shareholder),
				new Order(3, security, Side.BUY, 445, 15450, broker, shareholder),
				new Order(4, security, Side.BUY, 526, 15450, broker, shareholder),
				new Order(5, security, Side.BUY, 1000, 15400, broker, shareholder),
				new Order(6, security, Side.SELL, 350, 15800, broker, shareholder),
				new Order(7, security, Side.SELL, 285, 15810, broker, shareholder),
				new Order(8, security, Side.SELL, 800, 15810, broker, shareholder),
				new Order(9, security, Side.SELL, 340, 15820, broker, shareholder),
				new Order(10, security, Side.SELL, 65, 15820, broker, shareholder)
			);
		orders.forEach(order -> orderBook.enqueue(order));
	}

	@Test
	void new_sell_order_matches_completely_with_part_of_the_first_buy() {
		Order order = new Order(11, security, Side.SELL, 100, 15600, broker, shareholder);
		Trade trade = new Trade(security, 15700, 100, orders.get(0), order);
		List<Trade> trades = matcher.continuousMatch(order);
		assertThat(order.getQuantity()).isEqualTo(0);
		assertThat(trades).containsExactly(trade);
		assertThat(security.getOrderBook().getBuyQueue().getFirst().getQuantity()).isEqualTo(204);
	}

	@Test
	void new_sell_order_matches_partially_with_the_first_buy() {
		Order order = new Order(11, security, Side.SELL, 500, 15600, broker, shareholder);
		Trade trade = new Trade(security, 15700, 304, orders.get(0), order);
		List<Trade> trades = matcher.continuousMatch(order);
		assertThat(order.getQuantity()).isEqualTo(196);
		assertThat(trades).containsExactly(trade);
		assertThat(security.getOrderBook().getBuyQueue().getFirst().getOrderId()).isEqualTo(2);
	}

	@Test
	void new_buy_order_does_not_match() {
		Order order = new Order(11, security, Side.BUY, 2000, 15500, broker, shareholder);
		List<Trade> trades = matcher.continuousMatch(order);
		assertThat(order).isEqualTo(order);
		assertThat(trades).isEmpty();
	}
}
