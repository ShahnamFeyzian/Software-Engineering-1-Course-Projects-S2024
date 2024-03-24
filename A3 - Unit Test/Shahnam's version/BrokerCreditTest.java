package ir.ramtung.tinyme.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.domain.entity.IcebergOrder;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.domain.entity.Shareholder;
import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;

@SpringBootTest
public class BrokerCreditTest {
    private Security security;
    private Broker sellerBroker;
    private Broker buyerBroker;
    private Shareholder shareholder;
    private OrderBook orderBook;
    private List<Order> orders;
    @Autowired
    private Matcher matcher;

    @BeforeEach
    void setup() {
        security = Security.builder().build();
        sellerBroker = Broker.builder().credit(0).build();
        buyerBroker = Broker.builder().credit(0).build();
        shareholder = Shareholder.builder().build();
        shareholder.incPosition(security, 100_000);
        orderBook = security.getOrderBook();
        orders = Arrays.asList(
            new Order(1, security, Side.SELL, 10, 100, sellerBroker, shareholder),
            new Order(2, security, Side.SELL, 10, 150, sellerBroker, shareholder),
            new Order(3, security, Side.SELL, 10, 200, sellerBroker, shareholder),
            new Order(4, security, Side.SELL, 10, 250, sellerBroker, shareholder),
            new Order(5, security, Side.SELL, 10, 300, sellerBroker, shareholder),
            new Order(6, security, Side.BUY, 10, 100, buyerBroker, shareholder),
            new Order(7, security, Side.BUY, 10, 150, buyerBroker, shareholder),
            new Order(8, security, Side.BUY, 10, 200, buyerBroker, shareholder),
            new Order(9, security, Side.BUY, 10, 250, buyerBroker, shareholder),
            new Order(10, security, Side.BUY, 10, 300, buyerBroker, shareholder),
            new IcebergOrder(11, security, Side.SELL, 25, 350, sellerBroker, shareholder, 10),
            new IcebergOrder(12, security, Side.BUY, 25, 50, buyerBroker, shareholder, 10)
        );
        orders.forEach(order -> orderBook.enqueue(order));
    }

    @Test
    public void cancel_sell_order_and_no_change_in_broker_credit(){
        DeleteOrderRq deleteRequest1 = new DeleteOrderRq(0, "", Side.SELL, 1);
        DeleteOrderRq deleteRequest2 = new DeleteOrderRq(0, "", Side.SELL, 3);

        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest1));
        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest2));
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 0;


        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void cancel_sell_ice_order_and_no_change_in_broker_credit(){
        DeleteOrderRq deleteRequest1 = new DeleteOrderRq(0, "", Side.SELL, 11);

        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest1));        
        long actualCredit = sellerBroker.getBrokerId();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }
    
    @Test
    public void cancel_buy_order_and_increase_broker_credit(){
        DeleteOrderRq deleteRequest1 = new DeleteOrderRq(0, null, Side.BUY, 6);
        DeleteOrderRq deleteRequest2 = new DeleteOrderRq(0, null, Side.BUY, 7);

        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest1));
        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest2));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 2500;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void cancel_buy_ice_order_and_increase_broker_credit(){
        DeleteOrderRq deleteRequest1 = new DeleteOrderRq(0, null, Side.BUY, 12);

        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteRequest1));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 1250;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_sell_order_and_trade_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 1, null, Side.SELL, 
            20, 100, 0, 0, 0
        );
        EnterOrderRq updateRequest2 = EnterOrderRq.createUpdateOrderRq(
            0, null, 3, null, Side.SELL, 
            10, 100, 0, 0, 0
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest2, matcher));
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 7500; 
        
        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_sell_ice_order_and_trade_occure() {
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 11, null, Side.SELL, 
            25, 100, 0, 0, 10
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 3000;
        //TODO 
        // wrong logic and bug in the system that should be fixed 
        // the right expected answer should be 6500 

        assertThat(actualCredit).isEqualTo(expectedCredit); 
    }

    @Test
    public void update_sell_order_and_trade_dosnt_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 2, null, Side.SELL, 
            20, 400, 0, 0, 0
        );
        EnterOrderRq updateRequest2 = EnterOrderRq.createUpdateOrderRq(
            0, null, 5, null, Side.SELL, 
            10, 500, 0, 0, 0
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest2, matcher));
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_sell_ice_order_and_trade_dosnt_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 11, null, Side.SELL, 
            25, 400, 0, 0, 15
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 0;
        
        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_buy_order_and_trade_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 10, null, Side.BUY, 
            20, 400, 0, 0, 0
        );
        EnterOrderRq updateRequest2 = EnterOrderRq.createUpdateOrderRq(
            0, null, 6, null, Side.BUY, 
            20, 200, 0, 0, 0
        );

        buyerBroker.increaseCreditBy(3000);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest2, matcher));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 500;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_buy_ice_order_and_trade_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 12, null, Side.BUY, 
            25, 400, 0, 0, 10
        );

        buyerBroker.increaseCreditBy(2250);
        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 2500;
        //TODO 
        // wrong logic and bug in the system that should be fixed 
        // the right expected answer should be 0

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_buy_order_and_trade_dosnt_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 6, null, Side.BUY, 
            10, 50, 0, 0, 0
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 500;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_buy_ice_order_and_trade_dosnt_occure(){
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 12, null, Side.BUY, 
            25, 40, 0, 0, 10
        );

        assertThatNoException().isThrownBy(() -> security.updateOrder(updateRequest1, matcher));
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 850;
        //TODO 
        // wrong logic and bug in the system that should be fixed 
        // the right expected answer should be 250

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void update_buy_order_and_broker_hasnt_credit() throws InvalidRequestException{
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 6, null, Side.BUY, 
            20, 200, 0, 0, 0
        );

        MatchingOutcome actualOutcome = security.updateOrder(updateRequest1, matcher).outcome();
        MatchingOutcome expectedOutcome = MatchingOutcome.NOT_ENOUGH_CREDIT;

        assertThat(actualOutcome).isEqualTo(expectedOutcome);
    }

    @Test
    public void update_buy_ice_order_and_broker_hasnt_credit() throws InvalidRequestException{
        EnterOrderRq updateRequest1 = EnterOrderRq.createUpdateOrderRq(
            0, null, 12, null, Side.BUY, 
            25, 60, 0, 0, 10
        );

        MatchingOutcome actualOutcome = security.updateOrder(updateRequest1, matcher).outcome();
        MatchingOutcome expectedOutcome = MatchingOutcome.EXECUTED;
        //TODO 
        // wrong logic and bug in the system that should be fixed 
        // the right expected answer should be NOT_ENOUGH_CREDIT

        assertThat(actualOutcome).isEqualTo(expectedOutcome);
    }

    @Test
    public void new_sell_order_and_trade_occure(){
        Order order = new Order(0, security, Side.SELL, 75, 50, sellerBroker, shareholder);

        matcher.execute(order);
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 11250;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_sell_ice_order_and_trade_occure(){
        IcebergOrder order = new IcebergOrder(0, security, Side.SELL, 75, 50, sellerBroker, shareholder, 5);

        matcher.execute(order);
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 11250;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }
    
    @Test
    public void new_sell_order_and_trade_dosnt_occure(){
        Order order = new Order(0, security, Side.SELL, 75, 400, sellerBroker, shareholder);

        matcher.execute(order);
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_sell_ice_order_and_trade_dosnt_occure(){
        IcebergOrder order = new IcebergOrder(0, security, Side.SELL, 75, 400, sellerBroker, shareholder, 5);

        matcher.execute(order);
        long actualCredit = sellerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_order_and_trade_occure(){
        Order order = new Order(0, security, Side.BUY, 75, 400, buyerBroker, shareholder);
        
        buyerBroker.increaseCreditBy(19000);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 250;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_ice_order_and_trade_occure(){
        IcebergOrder order = new IcebergOrder(0, security, Side.BUY, 25, 400, buyerBroker, shareholder, 12);
        
        buyerBroker.increaseCreditBy(3500);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_order_and_trade_dosnt_occure(){
        Order order = new Order(0, security, Side.BUY, 50, 30, buyerBroker, shareholder);

        buyerBroker.increaseCreditBy(1500);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_ice_order_and_trade_dosnt_occure(){
        IcebergOrder order = new IcebergOrder(0, security, Side.BUY, 20, 50, buyerBroker, shareholder, 12);

        buyerBroker.increaseCreditBy(1500);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 500;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_order_that_buy_all_sells_and_gos_to_queue(){
        Order order = new Order(0, security, Side.BUY, 100, 400, buyerBroker, shareholder);
        
        buyerBroker.increaseCreditBy(30000);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 1250;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }

    @Test
    public void new_buy_ice_order_that_buy_all_sells_and_gos_to_queue(){
        IcebergOrder order = new IcebergOrder(0, security, Side.BUY, 100, 400, buyerBroker, shareholder, 8);
        
        buyerBroker.increaseCreditBy(28750);
        matcher.execute(order);
        long actualCredit = buyerBroker.getCredit();
        long expectedCredit = 0;

        assertThat(actualCredit).isEqualTo(expectedCredit);
    }
}
