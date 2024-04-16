package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.service.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
class SecurityTest {
    private Security security;
    private Broker broker;
    private Shareholder shareholder;
    private List<Order> orders;
    @Autowired
    Matcher matcher;
    @BeforeEach
    void setupOrderBook() {
        security = Security.builder().build();
        broker = Broker.builder().brokerId(0).credit(36_841_250L).build();
        shareholder = Shareholder.builder().shareholderId(0).build();
        shareholder.incPosition(security, 100_000);
        orders = Arrays.asList(
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
        orders.forEach(order -> security.getOrderBook().enqueue(order));
    }

    @Test
    void reducing_quantity_does_not_change_priority() {
        Order updateOrder = new Order(3, security, Side.BUY, 440, 15450, broker, shareholder);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateOrder, matcher));
        assertThat(security.getOrderBook().getBuyQueue().get(2).getQuantity()).isEqualTo(440);
        assertThat(security.getOrderBook().getBuyQueue().get(2).getOrderId()).isEqualTo(3);
    }

    @Test
    void increasing_quantity_changes_priority() {
        Order updateOrder = new Order(3, security, Side.BUY, 450, 15450, broker, shareholder);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateOrder, matcher));
        assertThat(security.getOrderBook().getBuyQueue().get(3).getQuantity()).isEqualTo(450);
        assertThat(security.getOrderBook().getBuyQueue().get(3).getOrderId()).isEqualTo(3);
    }

    @Test
    void changing_price_changes_priority() {
        Order updateOrder = new Order(1, security, Side.BUY, 300, 15450, broker, shareholder);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateOrder, matcher));
        assertThat(security.getOrderBook().getBuyQueue().get(3).getQuantity()).isEqualTo(300);
        assertThat(security.getOrderBook().getBuyQueue().get(3).getPrice()).isEqualTo(15450);
        assertThat(security.getOrderBook().getBuyQueue().get(3).getOrderId()).isEqualTo(1);
        assertThat(security.getOrderBook().getBuyQueue().get(0).getOrderId()).isEqualTo(2);
    }
    @Test
    void changing_price_causes_trades_to_happen() {
        Order updateOrder = new Order(6, security, Side.SELL, 350, 15700, broker, shareholder);
        assertThatNoException().isThrownBy(() ->
                assertThat(security.updateOrder(updateOrder, matcher).getFirst().trades()).isNotEmpty()
        );
    }

    @Test
    void updating_non_existing_order_fails() {
        Order updateOrder = new Order(6, security, Side.BUY, 350, 15700, broker, shareholder);
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> security.updateOrder(updateOrder, matcher));
    }

    @Test
    void delete_order_works() {
        assertThatNoException().isThrownBy(() -> security.deleteOrder(Side.SELL, 6));
        assertThat(security.getOrderBook().getBuyQueue()).isEqualTo(orders.subList(0, 5));
        assertThat(security.getOrderBook().getSellQueue()).isEqualTo(orders.subList(6, 10));
    }

    @Test
    void deleting_non_existing_order_fails() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> security.deleteOrder(Side.SELL, 1));
    }

    @Test
    void increasing_iceberg_peak_size_changes_priority() {
        security = Security.builder().build();
        broker = Broker.builder().credit(1_000_000L).build();
        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 15700, broker, shareholder),
                new Order(2, security, Side.BUY, 43, 15500, broker, shareholder),
                new IcebergOrder(3, security, Side.BUY, 445, 15450, broker, shareholder, 100),
                new Order(4, security, Side.BUY, 526, 15450, broker, shareholder),
                new Order(5, security, Side.BUY, 1000, 15400, broker, shareholder)
        );
        broker.increaseCreditBy(35_841_250);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        Order updateOrder = new IcebergOrder(3, security, Side.BUY, 445, 15450, broker, shareholder, 150);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateOrder, matcher));
        assertThat(security.getOrderBook().getBuyQueue().get(3).getQuantity()).isEqualTo(150);
        assertThat(security.getOrderBook().getBuyQueue().get(3).getOrderId()).isEqualTo(3);
    }

    @Test
    void decreasing_iceberg_quantity_to_amount_larger_than_peak_size_does_not_changes_priority() {
        security = Security.builder().build();
        broker = Broker.builder().build();
        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 15700, broker, shareholder),
                new Order(2, security, Side.BUY, 43, 15500, broker, shareholder),
                new IcebergOrder(3, security, Side.BUY, 445, 15450, broker, shareholder, 100),
                new Order(4, security, Side.BUY, 526, 15450, broker, shareholder),
                new Order(5, security, Side.BUY, 1000, 15400, broker, shareholder)
        );
        broker.increaseCreditBy(35_841_250);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        Order updateOrder = new IcebergOrder(3, security, Side.BUY, 300, 15450, broker, shareholder, 100);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateOrder, matcher));
        assertThat(security.getOrderBook().getBuyQueue().get(2).getOrderId()).isEqualTo(3);
    }

}