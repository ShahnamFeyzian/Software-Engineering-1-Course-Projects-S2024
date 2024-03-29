package ir.ramtung.tinyme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.service.Matcher;

@SpringBootTest
public class SecurityTest {
    private Security security;
    private Broker sellerBroker;
    private Broker buyerBroker;
    private Shareholder sellerShareholder;
    private Shareholder buyerShareholder;
    private OrderBook orderBook;
    private List<Order> orders;
    @Autowired
    private Matcher matcher;

    private static class AssertingPack {
        private static Security security;
        private static Broker sellerBroker;
        private static Broker buyerBroker;
        private static Shareholder sellerShareholder;
        private static Shareholder buyerShareholder;
        private static long exceptedSellerCredit;
        private static long exceptedBuyerCredit;
        private static Integer exceptedSellerPosition;
        private static Integer exceptedBuyerPosition;
        private static LinkedList<Order> sellQueue;
        private static LinkedList<Order> buyQueue;

        private static void initialize() {
            exceptedSellerCredit = sellerBroker.getCredit();
            exceptedBuyerCredit = buyerBroker.getCredit();
            exceptedSellerPosition = sellerShareholder.getPositionBySecurity(security);
            exceptedBuyerPosition = buyerShareholder.getPositionBySecurity(security);
        }

        private static void assertSellerCredit() {
            assertThat(sellerBroker.getCredit()).isEqualTo(exceptedSellerCredit);
        }

        private static void assertBuyerCredit() {
            assertThat(buyerBroker.getCredit()).isEqualTo(exceptedBuyerCredit);
        }

        private static void assertSellerPosition() {
            assertThat(sellerShareholder.getPositionBySecurity(security)).isEqualTo(exceptedSellerPosition);
        }

        private static void assertBuyerPosition() {
            assertThat(buyerShareholder.getPositionBySecurity(security)).isEqualTo(exceptedBuyerPosition);
        }

        private static void assertCredits() {
            assertSellerCredit();
            assertBuyerCredit();
        }

        private static void assertPositions() {
            assertSellerPosition();
            assertBuyerPosition();
        }

        private static void assertAll() {
            assertCredits();
            assertPositions();
        }

        private static void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int price) {
            Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
            long actualId = order.getOrderId();
            int actualquantity = order.getTotalQuantity();
            int actualPrice = order.getPrice();

            assertThat(actualId).isEqualTo(orderId);
            assertThat(actualquantity).isEqualTo(quantity);
            assertThat(actualPrice).isEqualTo(price);
        }

        private static void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int price, int peakSize, int displayedQuantity) {
            assertOrderInQueue(side, idx, orderId, quantity, price);
            Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
            IcebergOrder iceOrder = (IcebergOrder) order;
            int actualPeakSize = iceOrder.getPeakSize(); 
            int actualDisplayedQuantity = iceOrder.getDisplayedQuantity();

            assertThat(actualPeakSize).isEqualTo(peakSize);
            assertThat(actualDisplayedQuantity).isEqualTo(displayedQuantity);
        }
    }

    @BeforeEach
    void setup() {
        security = Security.builder().build();
        sellerBroker = Broker.builder().credit(0).build();
        buyerBroker = Broker.builder().credit(32500).build();
        sellerShareholder = Shareholder.builder().build();
        buyerShareholder = Shareholder.builder().build();
        sellerShareholder.incPosition(security, 85);
        buyerShareholder.incPosition(security, 0);
        orderBook = security.getOrderBook();
        orders = Arrays.asList(
            new Order(1, security, Side.BUY, 10, 100, buyerBroker, buyerShareholder),
            new Order(2, security, Side.BUY, 10, 200, buyerBroker, buyerShareholder),
            new Order(3, security, Side.BUY, 10, 300, buyerBroker, buyerShareholder),
            new Order(4, security, Side.BUY, 10, 400, buyerBroker, buyerShareholder),
            new IcebergOrder(5, security, Side.BUY, 45, 500, buyerBroker, buyerShareholder, 10),
            new Order(1, security, Side.SELL, 10, 600, sellerBroker, sellerShareholder),
            new Order(2, security, Side.SELL, 10, 700, sellerBroker, sellerShareholder),
            new Order(3, security, Side.SELL, 10, 800, sellerBroker, sellerShareholder),
            new Order(4, security, Side.SELL, 10, 900, sellerBroker, sellerShareholder),
            new IcebergOrder(5, security, Side.SELL, 45, 1000, sellerBroker, sellerShareholder, 10)
        );
        orders.forEach(order -> orderBook.enqueue(order));
        AssertingPack.security = this.security;
        AssertingPack.sellerBroker = this.sellerBroker;
        AssertingPack.buyerBroker = this.buyerBroker;
        AssertingPack.sellerShareholder = this.sellerShareholder;
        AssertingPack.buyerShareholder = this.buyerShareholder;
        AssertingPack.sellQueue = orderBook.getSellQueue();
        AssertingPack.buyQueue = orderBook.getBuyQueue();
        AssertingPack.initialize();
    }

    @Test
    public void delete_sell_order() {
        security.deleteOrder(Side.SELL, 2);
        
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 3, 10, 800);
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 2, 10, 200);
    }

    @Test
    public void delete_sell_ice_order() {
        security.deleteOrder(Side.SELL, 5);

        AssertingPack.assertAll();
        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> orderBook.getSellQueue().get(4));
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void delete_buy_order() {
        security.deleteOrder(Side.BUY, 3);
        
        AssertingPack.exceptedBuyerCredit = 3000;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 2, 10, 200);
    }

    @Test
    public void delete_buy_ice_order() {
        security.deleteOrder(Side.BUY, 5);

        AssertingPack.exceptedBuyerCredit = 22500;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void delete_non_existing_order() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> security.deleteOrder(Side.SELL, 6));
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> security.deleteOrder(Side.BUY, 8));
        AssertingPack.assertAll();
    }

    @Test
    public void decrease_sell_order_quantity() {
        Order order = new Order(1, security, Side.SELL, 4, 600, sellerBroker, sellerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 4, 600);
        // TODO
        // what if new quantity be zero? what should happend in that case?
    }

    @Test 
    public void decrease_sell_ice_order_quantity() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 30, 1000, sellerBroker, sellerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 30, 1000, 10, 10);
    }

    @Test
    public void decrease_buy_order_quantity() {
        Order order = new Order(3, security, Side.BUY, 7, 300, buyerBroker, buyerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerCredit = 900;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 3, 7, 300);
    }

    @Test 
    public void decrease_buy_ice_order_quantity() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 7, 500, sellerBroker, sellerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerCredit = 19000;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 7, 500, 10, 7);
    }

    @Test
    public void increase_sell_order_quantity() {
        Order order = new Order(2, security, Side.SELL, 15, 700, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 5);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerPosition = 90;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 2, 15, 700);
    }

    @Test
    public void increase_sell_ice_order_quantity() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 60, 1000, sellerBroker, sellerShareholder, 10);
        sellerShareholder.incPosition(security, 15);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerPosition = 100;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 60, 1000, 10, 10);
    } 

    @Test
    public void increase_sell_order_quantity_but_not_enough_position() {
        Order order = new Order(2, security, Side.SELL, 15, 700, sellerBroker, sellerShareholder);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
    }

    @Test
    public void increase_sell_ice_order_quantity_but_not_enough_position() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 60, 1000, sellerBroker, sellerShareholder, 10);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    @Test
    public void increase_buy_order_quantity() {
        Order order = new Order(4, security, Side.BUY, 25, 400, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(6000);
        security.updateOrder(order, matcher);
        
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 1, 4, 25, 400);
    }

    @Test
    public void increase_buy_ice_order_quantity() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 60, 500, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(7500);
        security.updateOrder(order, matcher);
        
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 60, 500, 10, 10);
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit() {
        Order order = new Order(4, security, Side.BUY, 25, 400, buyerBroker, buyerShareholder);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.BUY, 1, 4, 10, 400);
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 60, 500, buyerBroker, buyerShareholder, 10);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();
        
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }


    // TODO
    // add peakSize scenarios after you are sure how they work


    @Test
    public void decrease_sell_order_price_no_trading_happens() {
        Order order = new Order(3, security, Side.SELL, 10, 650, sellerBroker, sellerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 3, 10, 650);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 2, 10, 700);
    }

    @Test
    public void decrease_sell_ice_order_price_no_trading_happens() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 600, sellerBroker, sellerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 5, 45, 600, 10, 10);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 2, 10, 700);
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded() {
        Order order = new Order(3, security, Side.SELL, 10, 450, sellerBroker, sellerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerCredit = 5000;
        AssertingPack.exceptedBuyerPosition = 10;
        AssertingPack.exceptedSellerPosition = 75;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 4, 10, 900);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 3)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 35, 500, 10, 10);
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 450, sellerBroker, sellerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerCredit = 22500;
        AssertingPack.exceptedBuyerPosition = 45;
        AssertingPack.exceptedSellerPosition = 40;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 5)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded() {
        Order order = new Order(3, security, Side.SELL, 50, 450, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 40);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerCredit = 22500;
        AssertingPack.exceptedBuyerPosition = 45;
        AssertingPack.exceptedSellerPosition = 80;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 3, 5, 450);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 50, 450, sellerBroker, sellerShareholder, 10);
        sellerShareholder.incPosition(security, 5);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedSellerCredit = 22500;
        AssertingPack.exceptedBuyerPosition = 45;
        AssertingPack.exceptedSellerPosition = 45;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 5, 5, 450, 10, 5);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
    }

    @Test
    public void decrease_buy_order_price() {
        Order order = new Order(3, security, Side.BUY, 10, 150, buyerBroker, buyerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerCredit = 1500;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 3, 10, 150);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 2, 10, 200);
    }

    @Test
    public void decrease_buy_ice_order_price() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 200, buyerBroker, buyerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerCredit = 13500;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 5, 45, 200, 10, 10);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 2, 10, 200);
    }

    @Test
    public void increase_sell_order_price() {
        Order order = new Order(3, security, Side.SELL, 10, 950, sellerBroker, sellerShareholder);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
        AssertingPack.assertOrderInQueue(Side.SELL, 3, 3, 10, 950);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 4, 10, 900);
    }

    @Test
    public void increase_sell_ice_order_price() {
        IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 1100, sellerBroker, sellerShareholder, 10);
        security.updateOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1100, 10, 10);
        AssertingPack.assertOrderInQueue(Side.SELL, 3, 4, 10, 900);
    }

    @Test
    public void increase_buy_order_price_no_trading_happens() {
        Order order = new Order(1, security, Side.BUY, 10, 250, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(1500);
        security.updateOrder(order, matcher);
    
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 4, 2, 10, 200);
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 250);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 550, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(2250);
        security.updateOrder(order, matcher);
    
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 550, 10, 10);
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit() {
        Order order = new Order(1, security, Side.BUY, 10, 250, buyerBroker, buyerShareholder);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();
    
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 550, buyerBroker, buyerShareholder, 10);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();
    
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void increase_buy_order_price_and_completely_traded() {
        Order order = new Order(2, security, Side.BUY, 10, 600, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(5000);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 10;
        AssertingPack.exceptedSellerCredit = 6000;
        AssertingPack.exceptedSellerPosition = 75;
        AssertingPack.exceptedBuyerCredit = 1000;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 2)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 1)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 100);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 1000, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(12500);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 45;
        AssertingPack.exceptedSellerCredit = 35000;
        AssertingPack.exceptedSellerPosition = 40;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 5, 40, 1000, 10, 5);
    }

    @Test
    public void increase_buy_order_price_and_partially_traded() {
        Order order = new Order(3, security, Side.BUY, 25, 700, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(13500);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 20;
        AssertingPack.exceptedSellerCredit = 13000;
        AssertingPack.exceptedSellerPosition = 65;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 1)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 2)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 3, 5, 700);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 3, 10, 800);
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 90, 1000, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(80000);
        security.updateOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 85;
        AssertingPack.exceptedSellerCredit = 75000;
        AssertingPack.exceptedSellerPosition = 0;
        AssertingPack.exceptedBuyerCredit = 22500;
        AssertingPack.assertAll();
        assertThat(orderBook.getSellQueue().size()).isZero();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 5, 1000, 10, 5);
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit() {
        Order order = new Order(3, security, Side.BUY, 25, 800, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(13500);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();

        AssertingPack.exceptedBuyerCredit = 13500;
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 3, 10, 800);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit() {
        IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 90, 1000, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(57000);
        MatchingOutcome res = security.updateOrder(order, matcher).outcome();

        AssertingPack.exceptedBuyerCredit = 57000;
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 3, 10, 800);
        AssertingPack.assertOrderInQueue(Side.SELL, 3, 4, 10, 900);
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500);
    }

    @Test
    public void add_sell_order_no_trades_happens() {
        Order order = new Order(6, security, Side.SELL, 15, 650, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 15);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedSellerPosition = 100;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 1, 6, 15, 650);
        AssertingPack.assertOrderInQueue(Side.SELL, 2, 2, 10, 700);
    }

    @Test
    public void add_sell_ice_order_no_trades_happens() {
        IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 20, 1000, sellerBroker, sellerShareholder, 7);
        sellerShareholder.incPosition(security, 20);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedSellerPosition = 105;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
        AssertingPack.assertOrderInQueue(Side.SELL, 5, 6, 20, 1000, 7, 7);
    }

    @Test
    public void add_sell_order_and_not_enough_position() {
        Order order = new Order(6, security, Side.SELL, 15, 650, sellerBroker, sellerShareholder);
        MatchingOutcome res =  security.addNewOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position() {
        IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 20, 1000, sellerBroker, sellerShareholder, 7);
        MatchingOutcome res =  security.addNewOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_order_and_copletely_traded() {
        Order order = new Order(8, security, Side.SELL, 13, 400, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 13);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 13;
        AssertingPack.exceptedSellerCredit = 6500;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 8)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 32, 500, 10, 7);
    }

    @Test
    public void add_sell_ice_order_and_copletely_traded() {
        IcebergOrder order = new IcebergOrder(8, security, Side.SELL, 67, 100, sellerBroker, sellerShareholder, 9);
        sellerShareholder.incPosition(security, 67);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 67;
        AssertingPack.exceptedSellerCredit = 29900;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 8)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 2, 8, 200);
    }

    @Test
    public void add_sell_order_and_partially_traded() {
        Order order = new Order(7, security, Side.SELL, 60, 500, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 60);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 45;
        AssertingPack.exceptedSellerCredit = 22500;
        AssertingPack.exceptedSellerPosition = 100;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 7, 15, 500);
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size() {
        IcebergOrder order = new IcebergOrder(7, security, Side.SELL, 60, 400, sellerBroker, sellerShareholder, 3);
        sellerShareholder.incPosition(security, 60);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 55;
        AssertingPack.exceptedSellerCredit = 26500;
        AssertingPack.exceptedSellerPosition = 90;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 4)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 3, 10, 300);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 7, 5, 400, 3, 3);
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size() {
        IcebergOrder order = new IcebergOrder(7, security, Side.SELL, 60, 400, sellerBroker, sellerShareholder, 7);
        sellerShareholder.incPosition(security, 60);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 55;
        AssertingPack.exceptedSellerCredit = 26500;
        AssertingPack.exceptedSellerPosition = 90;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 4)).isFalse();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 3, 10, 300);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 7, 5, 400, 7, 5);
    }

    @Test
    public void add_sell_order_matches_with_all_buyers_queue_and_finished() {
        Order order = new Order(6, security, Side.SELL, 85, 100, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 85);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 85;
        AssertingPack.exceptedSellerCredit = 32500;
        AssertingPack.assertAll();
        assertThat(orderBook.getBuyQueue().size()).isZero();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyers_queue_and_finished() {
        IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 85, 100, sellerBroker, sellerShareholder, 10);
        sellerShareholder.incPosition(security, 85);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 85;
        AssertingPack.exceptedSellerCredit = 32500;
        AssertingPack.assertAll();
        assertThat(orderBook.getBuyQueue().size()).isZero();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_order_matches_with_all_buyers_queue_and_not_finished() {
        Order order = new Order(6, security, Side.SELL, 120, 100, sellerBroker, sellerShareholder);
        sellerShareholder.incPosition(security, 120);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 85;
        AssertingPack.exceptedSellerCredit = 32500;
        AssertingPack.exceptedSellerPosition = 120;
        AssertingPack.assertAll();
        assertThat(orderBook.getBuyQueue().size()).isZero();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 6, 35, 100);
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyers_queue_and_not_finished() {
        IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 100, 100, sellerBroker, sellerShareholder, 10);
        sellerShareholder.incPosition(security, 100);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 85;
        AssertingPack.exceptedSellerCredit = 32500;
        AssertingPack.exceptedSellerPosition = 100;
        AssertingPack.assertAll();
        assertThat(orderBook.getBuyQueue().size()).isZero();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 6, 15, 100, 10, 10);
    }

    @Test
    public void add_buy_order_no_trades_happens() {
        Order order = new Order(6, security, Side.BUY, 22, 300, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(6600);
        security.addNewOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 4, 2, 10, 200);
        AssertingPack.assertOrderInQueue(Side.BUY, 3, 6, 22, 300);
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test
    public void add_buy_ice_order_no_trades_happens() {
        IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 5, 450, buyerBroker, buyerShareholder, 1);
        buyerBroker.increaseCreditBy(2250);
        security.addNewOrder(order, matcher);

        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 2, 4, 10, 400);
        AssertingPack.assertOrderInQueue(Side.BUY, 1, 6, 5, 450, 1, 1);
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void add_buy_order_but_not_enough_credit() {
        Order order = new Order(10, security, Side.BUY, 22, 300, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(6000);
        MatchingOutcome res =  security.addNewOrder(order, matcher).outcome();

        AssertingPack.exceptedBuyerCredit = 6000;
        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 10));
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit() {
        IcebergOrder order = new IcebergOrder(10, security, Side.BUY, 5, 450, buyerBroker, buyerShareholder, 1);
        MatchingOutcome res =  security.addNewOrder(order, matcher).outcome();

        AssertingPack.assertAll();
        assertThat(res).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 10));
    }

    @Test
    public void add_buy_order_and_copletely_traded() {
        Order order = new Order(8, security, Side.BUY, 13, 700, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(8100);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 13;
        AssertingPack.exceptedSellerCredit = 8100;
        AssertingPack.exceptedSellerPosition = 72;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 8)).isFalse();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 2, 7, 700);
    }

    @Test
    public void add_buy_ice_order_and_copletely_traded() {
        IcebergOrder order = new IcebergOrder(8, security, Side.BUY, 52, 1100, buyerBroker, buyerShareholder, 10);
        buyerBroker.increaseCreditBy(42000);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 52;
        AssertingPack.exceptedSellerCredit = 42000;
        AssertingPack.exceptedSellerPosition = 33;
        AssertingPack.assertAll();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 8)).isFalse();
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 5, 33, 1000, 10, 8);
    }

    @Test
    public void add_buy_order_and_partially_traded() {
        Order order = new Order(6, security, Side.BUY, 13, 600, buyerBroker, buyerShareholder);
        buyerBroker.increaseCreditBy(7800);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 10;
        AssertingPack.exceptedSellerCredit = 6000;
        AssertingPack.exceptedSellerPosition = 75;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 6, 3, 600);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    @Test
    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size() {
        IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 13, 600, buyerBroker, buyerShareholder, 2);
        buyerBroker.increaseCreditBy(7800);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 10;
        AssertingPack.exceptedSellerCredit = 6000;
        AssertingPack.exceptedSellerPosition = 75;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 6, 3, 600, 2, 2);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    @Test
    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size() {
        IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 14, 600, buyerBroker, buyerShareholder, 5);
        buyerBroker.increaseCreditBy(8400);
        security.addNewOrder(order, matcher);

        AssertingPack.exceptedBuyerPosition = 10;
        AssertingPack.exceptedSellerCredit = 6000;
        AssertingPack.exceptedSellerPosition = 75;
        AssertingPack.assertAll();
        AssertingPack.assertOrderInQueue(Side.BUY, 0, 6, 4, 600, 5, 4);
        AssertingPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }
}
