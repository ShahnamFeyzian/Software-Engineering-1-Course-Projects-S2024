package ir.ramtung.tinyme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        private static long actualSellerCredit;
        private static long exceptedSellerCredit;
        private static long actualBuyerCredit;
        private static long exceptedBuyerCredit;
        private static Integer actualSellerPosition;
        private static Integer exceptedSellerPosition;
        private static Integer actualBuyerPosition;
        private static Integer exceptedBuyerPosition;
        
        private static void initialize() {
            actualSellerCredit = 0;
            exceptedSellerCredit = 0;
            actualBuyerCredit = 0;
            exceptedBuyerCredit = 0;
            actualSellerPosition = 0;
            exceptedSellerPosition = 0;
            actualBuyerPosition = 0;
            exceptedBuyerPosition = 0;
        }

        private static void assertSellerCredit() {
            assertThat(actualSellerCredit).isEqualTo(exceptedSellerCredit);
        }

        private static void assertBuyerCredit() {
            assertThat(actualBuyerCredit).isEqualTo(exceptedBuyerCredit);
        }

        private static void assertSellerPosition() {
            assertThat(actualSellerPosition).isEqualTo(exceptedSellerPosition);
        }

        private static void assertBuyerPosition() {
            assertThat(actualBuyerPosition).isEqualTo(exceptedBuyerPosition);
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
    }

    @BeforeEach
    void setup() {
        security = Security.builder().build();
        sellerBroker = Broker.builder().credit(0).build();
        buyerBroker = Broker.builder().credit(0).build();
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
        AssertingPack.initialize();
    }
}
