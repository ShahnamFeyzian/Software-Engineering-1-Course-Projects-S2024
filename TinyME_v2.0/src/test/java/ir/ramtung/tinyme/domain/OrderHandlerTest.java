package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.OrderHandler;
import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.OrderAcceptedEvent;
import ir.ramtung.tinyme.messaging.event.OrderExecutedEvent;
import ir.ramtung.tinyme.messaging.event.OrderRejectedEvent;
import ir.ramtung.tinyme.messaging.event.OrderUpdatedEvent;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
@DirtiesContext
public class OrderHandlerTest {
    @Autowired
    OrderHandler orderHandler;
    @Autowired
    EventPublisher eventPublisher;
    @Autowired
    SecurityRepository securityRepository;
    @Autowired
    BrokerRepository brokerRepository;
    @Autowired
    ShareholderRepository shareholderRepository;
    private Security security;
    private Shareholder shareholder;
    private Broker broker1;
    private Broker broker2;

    OrderRejectedEvent captureOrderRejectedEvent() {
        ArgumentCaptor<OrderRejectedEvent> orderRejectedCaptor = ArgumentCaptor.forClass(OrderRejectedEvent.class);
        verify(eventPublisher).publish(orderRejectedCaptor.capture());
        OrderRejectedEvent outputEvent = orderRejectedCaptor.getValue();

        return outputEvent;
    }

    @BeforeEach
    void setup() {
        securityRepository.clear();
        brokerRepository.clear();
        shareholderRepository.clear();

        security = Security.builder().isin("ABC").build();
        securityRepository.addSecurity(security);

        shareholder = Shareholder.builder().shareholderId(1).build();
        shareholderRepository.addShareholder(shareholder);

        broker1 = Broker.builder().brokerId(1).build();
        broker2 = Broker.builder().brokerId(2).build();
        brokerRepository.addBroker(broker1);
        brokerRepository.addBroker(broker2);
    }

    @Test
    void new_order_invalid_fields() {
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(0, "ABC", -1, null, null, -1, -1, 1, 1, -1, -1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.INVALID_ORDER_ID,
                Message.ORDER_QUANTITY_NOT_POSITIVE,
                Message.ORDER_PRICE_NOT_POSITIVE,
                Message.INVALID_PEAK_SIZE,
                Message.INVALID_MINIMUM_EXECUTION_QUANTITY,
                Message.SIDE_CAN_NOT_BE_NULL
        );
    }

    @Test
    void new_order_invalid_repos() {
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(0, "-1", 1, null, Side.BUY, 1, 1, -1, -1, 0, 1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.UNKNOWN_SECURITY_ISIN,
                Message.UNKNOWN_BROKER_ID,
                Message.UNKNOWN_SHAREHOLDER_ID
        );
    }

    @Test
    void new_order_invalid_quantity_and_price_due_to_lot_and_tick_size() {
        Security security2 = Security.builder().isin("ABC2").lotSize(3).tickSize(3).build();
        securityRepository.addSecurity(security2);
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(0, "ABC2", 1, null, Side.BUY, 4, 4, 1, 1, 0, 1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE,
                Message.PRICE_NOT_MULTIPLE_OF_TICK_SIZE
        );
    }

    @Test
    void update_order_id_not_found() {
        Order inQueueOrder = new Order(1, security, Side.BUY, 100, 100, broker1, shareholder);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(0, "ABC", 2, null, Side.BUY, 1, 1, 1, 1, 0, 0));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.ORDER_ID_NOT_FOUND
        );
    }
    
    @Test
    void update_order_invalid_peaksize() {
        Order inQueueOrder = new Order(1, security, Side.BUY, 100, 100, broker1, shareholder);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(0, "ABC", 1, null, Side.BUY, 5, 1, 1, 1, 1, 0));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.INVALID_PEAK_SIZE
        );
    }
    
    @Test
    void update_iceberg_order_invalid_peaksize() {
        Order inQueueOrder = new IcebergOrder(1, security, Side.BUY, 100, 100, broker1, shareholder, 10);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(0, "ABC", 1, null, Side.BUY, 5, 1, 1, 1, 0, 0));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.CANNOT_SPECIFY_0_PEAK_SIZE_FOR_A_ICEBERG_ORDER
        );
    }
    
    @Test
    void update_iceberg_order_invalid_min_exec() {
        Order inQueueOrder = new Order(1, security, Side.BUY, 100, 100, broker1, shareholder);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(0, "ABC", 1, null, Side.BUY, 5, 1, 1, 1, 0, 1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.CANNOT_UPDATE_MINIMUM_EXECUTION_QUANTITY
        );
    }
    
    @Test
    void delete_order_invalid_order_id() {
        Order inQueueOrder = new Order(1, security, Side.BUY, 100, 100, broker1, shareholder);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleDeleteOrder(new DeleteOrderRq(1, "ABC", Side.BUY, -1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.INVALID_ORDER_ID
        );
    }
    
    @Test
    void delete_order_invalid_fields() {
        Order inQueueOrder = new Order(1, security, Side.BUY, 100, 100, broker1, shareholder);
        broker1.increaseCreditBy(100 * 100);
        security.getOrderBook().enqueue(inQueueOrder);

        orderHandler.handleDeleteOrder(new DeleteOrderRq(1, "-1", null, -1));
        OrderRejectedEvent outputEvent = this.captureOrderRejectedEvent();
        
        assertThat(outputEvent.getErrors()).containsOnly(
                Message.INVALID_ORDER_ID,
                Message.SIDE_CAN_NOT_BE_NULL,
                Message.UNKNOWN_SECURITY_ISIN
        );
    }

    @Test
    void new_sell_order_without_enough_positions_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 10, 10, broker1, shareholder)  
        );
        broker1.increaseCreditBy(100);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.incPosition(security, 9);

        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 10, 5, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 200, List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS)));
    }

    @Test
    void update_sell_order_without_enough_positions_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 10, 15, broker1, shareholder),
                new Order(7, security, Side.SELL, 10, 16, broker2, shareholder)
        );
        broker1.increaseCreditBy(150);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.incPosition(security, 9);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(1, "ABC", 7, LocalDateTime.now(), Side.SELL, 10, 12, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 7, List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS)));
    }

    @Test
    void new_buy_order_without_enough_credit_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.SELL, 10, 10, broker2, shareholder)  
        );
        shareholder.incPosition(security, 11);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        
        broker1.increaseCreditBy(250 - 1);
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.BUY, 20, 15, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 200, List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT)));
    }

    @Test
    void update_buy_order_without_enough_credit_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 20, 15, broker1, shareholder),
                new Order(7, security, Side.SELL, 10, 20, broker2, shareholder)
        );
        broker1.increaseCreditBy(400);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.incPosition(security, 10);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(1, "ABC", 1, LocalDateTime.now(), Side.BUY, 20, 21, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 1, List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT)));
    }

    @Test
    void new_buy_order_without_enough_minimum_execution_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.SELL, 10, 10, broker2, shareholder)  
        );
        shareholder.incPosition(security, 11);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        
        broker1.increaseCreditBy(300);
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.BUY, 20, 15, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 11));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 200, List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET)));
    }

    @Test
    void new_sell_order_without_enough_minimum_execution_is_rejected() {
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 10, 10, broker2, shareholder)  
        );
        broker2.increaseCreditBy(100);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        
        shareholder.incPosition(security, 20);
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 20, 15, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 11));

        verify(eventPublisher).publish(new OrderRejectedEvent(1, 200, List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET)));
    }


    @Test
    void new_order_matched_completely_with_one_trade() {
        Order matchingBuyOrder = new Order(100, security, Side.BUY, 1000, 15500, broker1, shareholder);
        Order incomingSellOrder = new Order(200, security, Side.SELL, 300, 15450, broker2, shareholder);
        broker1.increaseCreditBy(15_500_000);
        shareholder.incPosition(security, 300);
        security.getOrderBook().enqueue(matchingBuyOrder);

        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 300, 15450, 2, shareholder.getShareholderId(), 0, 50));

        Trade trade = new Trade(security, matchingBuyOrder.getPrice(), incomingSellOrder.getQuantity(),
                matchingBuyOrder, incomingSellOrder);
        verify(eventPublisher).publish((new OrderAcceptedEvent(1, 200)));
        verify(eventPublisher).publish(new OrderExecutedEvent(1, 200, List.of(new TradeDTO(trade))));
    }

    @Test
    void new_order_queued_with_no_trade() {
        shareholder.incPosition(security, 300);
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 300, 15450, 2, shareholder.getShareholderId(), 0, 0));
        verify(eventPublisher).publish(new OrderAcceptedEvent(1, 200));
    }

    @Test
    void new_order_matched_partially_with_two_trades() {
        Order matchingBuyOrder1 = new Order(100, security, Side.BUY, 300, 15500, broker1, shareholder);
        Order matchingBuyOrder2 = new Order(110, security, Side.BUY, 300, 15500, broker1, shareholder);
        Order incomingSellOrder = new Order(200, security, Side.SELL, 1000, 15450, broker2, shareholder);
        shareholder.incPosition(security, 1000);
        broker1.increaseCreditBy(9_300_000);
        security.getOrderBook().enqueue(matchingBuyOrder1);
        security.getOrderBook().enqueue(matchingBuyOrder2);

        Trade trade1 = new Trade(security, matchingBuyOrder1.getPrice(), matchingBuyOrder1.getQuantity(),
                matchingBuyOrder1, incomingSellOrder);
        Trade trade2 = new Trade(security, matchingBuyOrder2.getPrice(), matchingBuyOrder2.getQuantity(),
                matchingBuyOrder2, incomingSellOrder.snapshotWithQuantity(700));
        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1,
                incomingSellOrder.getSecurity().getIsin(),
                incomingSellOrder.getOrderId(),
                incomingSellOrder.getEntryTime(),
                incomingSellOrder.getSide(),
                incomingSellOrder.getTotalQuantity(),
                incomingSellOrder.getPrice(),
                incomingSellOrder.getBroker().getBrokerId(),
                incomingSellOrder.getShareholder().getShareholderId(), 0, 600));

        verify(eventPublisher).publish(new OrderAcceptedEvent(1, 200));
        verify(eventPublisher).publish(new OrderExecutedEvent(1, 200, List.of(new TradeDTO(trade1), new TradeDTO(trade2))));
    }

    @Test
    void iceberg_order_behaves_normally_before_being_queued() {
        Order matchingBuyOrder = new Order(100, security, Side.BUY, 1000, 15500, broker1, shareholder);
        Order incomingSellOrder = new IcebergOrder(200, security, Side.SELL, 300, 15450, broker2, shareholder, 100);
        shareholder.incPosition(security, 300);
        broker1.increaseCreditBy(15_500_000);
        security.getOrderBook().enqueue(matchingBuyOrder);
        Trade trade = new Trade(security, matchingBuyOrder.getPrice(), incomingSellOrder.getQuantity(),
                matchingBuyOrder, incomingSellOrder);

        EventPublisher mockEventPublisher = mock(EventPublisher.class, withSettings().verboseLogging());
        OrderHandler myOrderHandler = new OrderHandler(securityRepository, brokerRepository, shareholderRepository, mockEventPublisher, new Matcher());
        myOrderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1,
                incomingSellOrder.getSecurity().getIsin(),
                incomingSellOrder.getOrderId(),
                incomingSellOrder.getEntryTime(),
                incomingSellOrder.getSide(),
                incomingSellOrder.getTotalQuantity(),
                incomingSellOrder.getPrice(),
                incomingSellOrder.getBroker().getBrokerId(),
                incomingSellOrder.getShareholder().getShareholderId(), 100, 150));

        verify(mockEventPublisher).publish(new OrderAcceptedEvent(1, 200));
        verify(mockEventPublisher).publish(new OrderExecutedEvent(1, 200, List.of(new TradeDTO(trade))));
    }

    @Test
    void update_order_causing_no_trades() {
        Order queuedOrder = new Order(200, security, Side.SELL, 500, 15450, broker1, shareholder);
        shareholder.incPosition(security, 1000);
        security.getOrderBook().enqueue(queuedOrder);
        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 1000, 15450, 1, shareholder.getShareholderId(), 0, 0));
        verify(eventPublisher).publish(new OrderUpdatedEvent(1, 200));
    }

    @Test
    void handle_valid_update_with_trades() {
        Order matchingOrder = new Order(1, security, Side.BUY, 500, 15450, broker1, shareholder);
        Order beforeUpdate = new Order(200, security, Side.SELL, 1000, 15455, broker2, shareholder);
        Order afterUpdate = new Order(200, security, Side.SELL, 500, 15450, broker2, shareholder);
        shareholder.incPosition(security, 1000);
        broker1.increaseCreditBy(7_725_000);
        security.getOrderBook().enqueue(matchingOrder);
        security.getOrderBook().enqueue(beforeUpdate);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.SELL, 1000, 15450, broker2.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        Trade trade = new Trade(security, 15450, 500, matchingOrder, afterUpdate);
        verify(eventPublisher).publish(new OrderUpdatedEvent(1, 200));
        verify(eventPublisher).publish(new OrderExecutedEvent(1, 200, List.of(new TradeDTO(trade))));
    }


    @Test
    void update_sell_order_with_enough_positions_is_executed() {
        Shareholder shareholder1 = Shareholder.builder().build();
        shareholder1.incPosition(security, 100_000);
        shareholderRepository.addShareholder(shareholder1);
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 570, broker1, shareholder1),
                new Order(2, security, Side.BUY, 430, 550, broker1, shareholder1),
                new Order(3, security, Side.BUY, 445, 545, broker1, shareholder1),
                new Order(6, security, Side.SELL, 350, 250, 580, broker1, shareholder),
                new Order(7, security, Side.SELL, 100, 581, broker2, shareholder)
        );
        broker1.increaseCreditBy(100_652_305);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.incPosition(security, 350);

        orderHandler.handleEnterOrder(EnterOrderRq.createUpdateOrderRq(1, "ABC", 6, LocalDateTime.now(), Side.SELL, 250, 570, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 250));

        verify(eventPublisher).publish(any(OrderExecutedEvent.class));
        assertThat(shareholder1.hasEnoughPositionsOn(security, 100_000 + 250)).isTrue();
        assertThat(shareholder.hasEnoughPositionsOn(security, 101)).isFalse();
    }

    @Test
    void new_buy_order_does_not_check_for_position() {
        shareholder.incPosition(security, 100_000);
        Shareholder shareholder1 = Shareholder.builder().build();
        shareholder1.incPosition(security, 100_000);
        shareholderRepository.addShareholder(shareholder1);
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 570, broker1, shareholder1),
                new Order(2, security, Side.BUY, 430, 550, broker1, shareholder1),
                new Order(3, security, Side.BUY, 445, 545, broker1, shareholder1),
                new Order(6, security, Side.SELL, 350, 580, broker1, shareholder),
                new Order(7, security, Side.SELL, 100, 581, broker2, shareholder)
        );
        broker1.increaseCreditBy(100_652_305);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.decPosition(security, 99_500);

        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 200, LocalDateTime.now(), Side.BUY, 500, 570, broker1.getBrokerId(), shareholder.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(any(OrderAcceptedEvent.class));
        assertThat(shareholder1.hasEnoughPositionsOn(security, 100_000)).isTrue();
        assertThat(shareholder.hasEnoughPositionsOn(security, 500)).isTrue();
    }

    @Test
    void update_buy_order_does_not_check_for_position() {
        shareholder.incPosition(security, 100_000);
        Shareholder shareholder1 = Shareholder.builder().build();
        shareholder1.incPosition(security, 100_000);
        shareholderRepository.addShareholder(shareholder1);
        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 570, broker1, shareholder1),
                new Order(2, security, Side.BUY, 430, 550, broker1, shareholder1),
                new Order(3, security, Side.BUY, 445, 545, broker1, shareholder1),
                new Order(6, security, Side.SELL, 350, 580, broker1, shareholder),
                new Order(7, security, Side.SELL, 100, 581, broker2, shareholder)
        );
        broker1.increaseCreditBy(100_652_305);
        orders.forEach(order -> security.getOrderBook().enqueue(order));
        shareholder.decPosition(security, 99_500);

        orderHandler.handleEnterOrder(EnterOrderRq.createNewOrderRq(1, "ABC", 3, LocalDateTime.now(), Side.BUY, 500, 545, broker1.getBrokerId(), shareholder1.getShareholderId(), 0, 0));

        verify(eventPublisher).publish(any(OrderAcceptedEvent.class));
        assertThat(shareholder1.hasEnoughPositionsOn(security, 100_000)).isTrue();
        assertThat(shareholder.hasEnoughPositionsOn(security, 500)).isTrue();
    }
}
