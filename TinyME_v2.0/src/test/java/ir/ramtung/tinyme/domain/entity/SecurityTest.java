package ir.ramtung.tinyme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDateTime;
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
    private AssertingPack assertPack;
    private ScenarioGenerator scenarioGenerator;
    @Autowired
    private Matcher matcher;


    // --------------------------------------------------------------------------------
    // Helper classes
    // --------------------------------------------------------------------------------

    // Helper class to assert the state of the system
    private class AssertingPack {
        private long exceptedSellerCredit;
        private long exceptedBuyerCredit;
        private Integer exceptedSellerPosition;
        private Integer exceptedBuyerPosition;
        private Integer exceptedLastTradePrice;
        private LinkedList<Order> sellQueue;
        private LinkedList<Order> buyQueue;
        private LinkedList<Order> sellStopLimitQueue;
        private LinkedList<Order> buyStopLimitQueue;

        private AssertingPack() {
            exceptedSellerCredit = SecurityTest.this.sellerBroker.getCredit();
            exceptedBuyerCredit = SecurityTest.this.buyerBroker.getCredit();
            exceptedSellerPosition = SecurityTest.this.sellerShareholder.getPositionBySecurity(security);
            exceptedBuyerPosition = SecurityTest.this.buyerShareholder.getPositionBySecurity(security);
            exceptedLastTradePrice = SecurityTest.this.security.getLastTradePrice();
            sellQueue = SecurityTest.this.orderBook.getSellQueue();
            buyQueue = SecurityTest.this.orderBook.getBuyQueue();
            sellStopLimitQueue = SecurityTest.this.orderBook.getStopLimitOrderSellQueue();
            buyStopLimitQueue = SecurityTest.this.orderBook.getStopLimitOrderBuyQueue();
        }

        private void assertSellerCredit() {
            assertThat(SecurityTest.this.sellerBroker.getCredit()).isEqualTo(exceptedSellerCredit);
        }

        private void assertBuyerCredit() {
            assertThat(SecurityTest.this.buyerBroker.getCredit()).isEqualTo(exceptedBuyerCredit);
        }

        private void assertSellerPosition() {
            assertThat(SecurityTest.this.sellerShareholder.getPositionBySecurity(security)).isEqualTo(exceptedSellerPosition);
        }

        private void assertBuyerPosition() {
            assertThat(SecurityTest.this.buyerShareholder.getPositionBySecurity(security)).isEqualTo(exceptedBuyerPosition);
        }

        private void assertLastTradePrice() {
            assertThat(SecurityTest.this.security.getLastTradePrice()).isEqualTo(exceptedLastTradePrice);
        }

        private void assertOrderInStopLimitQueue(Side side, int idx, long orderId, int quantity, int price, int stopPrice) {
            StopLimitOrder order = (StopLimitOrder) ((side == Side.BUY) ? buyStopLimitQueue.get(idx) : sellStopLimitQueue.get(idx));
            long actualId = order.getOrderId();
            int actualquantity = order.getTotalQuantity();
            int actualPrice = order.getPrice();
            int actualStopPrice = order.getStopPrice();

            assertThat(actualId).isEqualTo(orderId);
            assertThat(actualquantity).isEqualTo(quantity);
            assertThat(actualPrice).isEqualTo(price);
            assertThat(actualStopPrice).isEqualTo(stopPrice);
        }

        private void assertMatchResult(MatchResult result, MatchingOutcome outcome, long orderId, int numOfTrades) {
            MatchingOutcome actualOutcome = result.outcome();
            long actualOrderId = result.remainder().getOrderId();
            int actualNumOfTrades = result.trades().size();

            assertThat(actualOutcome).isEqualTo(outcome);
            assertThat(actualOrderId).isEqualTo(orderId);
            assertThat(actualNumOfTrades).isEqualTo(numOfTrades);
        }

        private void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int minexteQuantity, int price) {
            Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
            long actualId = order.getOrderId();
            int actualquantity = order.getTotalQuantity();
            int actualPrice = order.getPrice();
            int actualMinexteQuantity = order.getMinimumExecutionQuantity();

            assertThat(actualId).isEqualTo(orderId);
            assertThat(actualquantity).isEqualTo(quantity);
            assertThat(actualMinexteQuantity).isEqualTo(minexteQuantity);
            assertThat(actualPrice).isEqualTo(price);
        }

        private void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int price) {
            assertOrderInQueue(side, idx, orderId, quantity, 0, price);
        }

        private void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int minexteQuantity, int price, int peakSize, int displayedQuantity) {
            assertOrderInQueue(side, idx, orderId, quantity, minexteQuantity, price);
            Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
            IcebergOrder iceOrder = (IcebergOrder) order;
            int actualPeakSize = iceOrder.getPeakSize(); 
            int actualDisplayedQuantity = iceOrder.getDisplayedQuantity();

            assertThat(actualPeakSize).isEqualTo(peakSize);
            assertThat(actualDisplayedQuantity).isEqualTo(displayedQuantity);
        }

        private void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int price, int peakSize, int displayedQuantity) {
            assertOrderInQueue(side, idx, orderId, quantity, 0, price, peakSize, displayedQuantity);
        }
    }

    // Helper class to generate scenarios
    private class ScenarioGenerator {
        public void delete_sell_order() {
            security.deleteOrder(Side.SELL, 2);
        }

        public void delete_sell_ice_order() {
            security.deleteOrder(Side.SELL, 5);
        }

        public void delete_buy_order() {
            security.deleteOrder(Side.BUY, 3);
        }

        public void delete_buy_ice_order() {
            security.deleteOrder(Side.BUY, 5);
        }

        public void delete_non_existing_sell_order() {
            security.deleteOrder(Side.SELL, 6);
        }

        public void delete_non_existing_buy_order() {
            security.deleteOrder(Side.BUY, 8);
        }

        public MatchResult decrease_sell_order_quantity() {
            Order order = new Order(1, security, Side.SELL, 4, 600, sellerBroker, sellerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_ice_order_quantity() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 30, 1000, sellerBroker, sellerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();   
        }

        public MatchResult decrease_buy_order_quantity() {
            Order order = new Order(3, security, Side.BUY, 7, 300, buyerBroker, buyerShareholder);
            return security.updateOrder(order, matcher).getFirst();   
        }

        public MatchResult decrease_buy_ice_order_quantity() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 7, 500, buyerBroker, buyerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_order_quantity() {
            Order order = new Order(2, security, Side.SELL, 15, 700, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 5);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_ice_order_quantity() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 60, 1000, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 15);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_order_quantity_but_not_enough_position() {
            Order order = new Order(2, security, Side.SELL, 15, 700, sellerBroker, sellerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_ice_order_quantity_but_not_enough_position() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 60, 1000, sellerBroker, sellerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();            
        }

        public MatchResult increase_buy_order_quantity() {
            Order order = new Order(4, security, Side.BUY, 25, 400, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(6000);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_quantity() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 60, 500, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(7500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_quantity_but_not_enough_credit() {
            Order order = new Order(4, security, Side.BUY, 25, 400, buyerBroker, buyerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_quantity_but_not_enough_credit() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 60, 500, buyerBroker, buyerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_order_price_no_trading_happens() {
            Order order = new Order(3, security, Side.SELL, 10, 650, sellerBroker, sellerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_ice_order_price_no_trading_happens() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 600, sellerBroker, sellerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();   
        }

        public MatchResult decrease_sell_order_price_and_completely_traded() {
            Order order = new Order(3, security, Side.SELL, 10, 450, sellerBroker, sellerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_ice_order_price_and_completely_traded() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 450, sellerBroker, sellerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_order_price_and_partially_traded() {
            Order order = new Order(3, security, Side.SELL, 50, 450, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 40);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_sell_ice_order_price_and_partially_traded() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 50, 450, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 5);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_buy_order_price() {
            Order order = new Order(3, security, Side.BUY, 10, 150, buyerBroker, buyerShareholder);
            return security.updateOrder(order, matcher).getFirst();   
        }

        public MatchResult decrease_buy_ice_order_price() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 200, buyerBroker, buyerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_order_price() {
            Order order = new Order(3, security, Side.SELL, 10, 950, sellerBroker, sellerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_sell_ice_order_price() {
            IcebergOrder order = new IcebergOrder(5, security, Side.SELL, 45, 1100, sellerBroker, sellerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_price_no_trading_happens() {
            Order order = new Order(1, security, Side.BUY, 10, 250, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(1500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_price_no_trading_happens() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 550, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(2250);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_price_no_trading_happens_and_not_enough_credit() {
            Order order = new Order(1, security, Side.BUY, 10, 250, buyerBroker, buyerShareholder);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 550, buyerBroker, buyerShareholder, 10);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_price_and_completely_traded() {
            Order order = new Order(2, security, Side.BUY, 10, 600, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(5000);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_price_and_completely_traded() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 45, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(12500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_price_and_partially_traded() {
            Order order = new Order(3, security, Side.BUY, 25, 700, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(13500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_price_and_partially_traded() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 90, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(80000);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_order_price_and_trade_happens_but_not_enough_credit() {
            Order order = new Order(3, security, Side.BUY, 25, 800, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(13500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback() {
            IcebergOrder order = new IcebergOrder(5, security, Side.BUY, 90, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(57000);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_no_trades_happens() {
            Order order = new Order(6, security, Side.SELL, 15, 650, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 15);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_no_trades_happens() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 20, 1000, sellerBroker, sellerShareholder, 7);
            sellerShareholder.incPosition(security, 20);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_and_not_enough_position() {
            Order order = new Order(6, security, Side.SELL, 15, 650, sellerBroker, sellerShareholder);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_and_not_enough_position() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 20, 1000, sellerBroker, sellerShareholder, 7);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_and_completely_traded() {
            Order order = new Order(8, security, Side.SELL, 13, 400, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 13);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_and_completely_traded() {
            IcebergOrder order = new IcebergOrder(8, security, Side.SELL, 67, 100, sellerBroker, sellerShareholder, 9);
            sellerShareholder.incPosition(security, 67);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_and_partially_traded() {
            Order order = new Order(7, security, Side.SELL, 60, 500, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 60);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size() {
            IcebergOrder order = new IcebergOrder(7, security, Side.SELL, 60, 400, sellerBroker, sellerShareholder, 3);
            sellerShareholder.incPosition(security, 60);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size() {
            IcebergOrder order = new IcebergOrder(7, security, Side.SELL, 60, 400, sellerBroker, sellerShareholder, 7);
            sellerShareholder.incPosition(security, 60);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_matches_with_all_buyer_queue_and_finished() {
            Order order = new Order(6, security, Side.SELL, 85, 100, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 85);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_matches_with_all_buyer_queue_and_finished() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 85, 100, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 85);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_matches_with_all_buyer_queue_and_not_finished() {
            Order order = new Order(6, security, Side.SELL, 120, 100, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 120);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 100, 100, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 100);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_with_min_execution_quantity_and_next_go_to_queue() {
            Order order = new Order(6, security, Side.SELL, 50, 10, 500, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 50);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 50, 10, 500, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 50);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_not_enough_execution_cause_rollback() {
            Order order = new Order(6, security, Side.SELL, 60, 50, 500, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 60);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_not_enough_execution_cause_rollback() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 100, 70, 300, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 100);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_quantity_is_equal_to_min_execution_quantity() {
            Order order = new Order(6, security, Side.SELL, 50, 50, 300, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 50);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_ice_order_quantity_is_equal_to_min_execution_quantity() {
            IcebergOrder order = new IcebergOrder(6, security, Side.SELL, 50, 50, 300, sellerBroker, sellerShareholder, 10);
            sellerShareholder.incPosition(security, 50);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_no_trades_happens() {
            Order order = new Order(6, security, Side.BUY, 22, 300, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(6600);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_no_trades_happens() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 5, 450, buyerBroker, buyerShareholder, 1);
            buyerBroker.increaseCreditBy(2250);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_but_not_enough_credit() {
            Order order = new Order(10, security, Side.BUY, 22, 300, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(6000);
            return  security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_but_not_enough_credit() {
            IcebergOrder order = new IcebergOrder(10, security, Side.BUY, 5, 450, buyerBroker, buyerShareholder, 1);
            buyerBroker.increaseCreditBy(2000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_and_completely_traded() {
            Order order = new Order(8, security, Side.BUY, 13, 700, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(8100);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_and_completely_traded() {
            IcebergOrder order = new IcebergOrder(8, security, Side.BUY, 52, 1100, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(42000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_and_partially_traded() {
            Order order = new Order(6, security, Side.BUY, 13, 600, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(7800);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 13, 600, buyerBroker, buyerShareholder, 2);
            buyerBroker.increaseCreditBy(7800);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 14, 600, buyerBroker, buyerShareholder, 5);
            buyerBroker.increaseCreditBy(8400);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_not_enough_credit_causes_rollback() {
            Order order = new Order(6, security, Side.BUY, 15, 750, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(9000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_not_enough_credit_causes_rollback() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 90, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(78000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_matches_with_all_seller_queue_and_finished() {
            Order order = new Order(6, security, Side.BUY, 85, 1000, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(75000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_matches_with_all_seller_queue_and_finished() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 85, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(75000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_matches_with_all_seller_queue_and_not_finished() {
            Order order = new Order(8, security, Side.BUY, 100, 1000, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(90000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_matches_with_all_seller_queue_and_not_finished() {
            IcebergOrder order = new IcebergOrder(8, security, Side.BUY, 100, 1000, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(90000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_with_min_execution_quantity_and_next_go_to_queue() {
            Order order = new Order(6, security, Side.BUY, 22, 17, 700, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(14400);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 32, 20, 700, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(21400);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_not_enough_execution_cause_rollback() {
            Order order = new Order(6, security, Side.BUY, 60, 50, 600, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(36000);
            return security.addNewOrder(order, matcher).getFirst();   
        }

        public MatchResult add_buy_ice_order_not_enough_execution_cause_rollback() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 100, 70, 800, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(80000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_order_quantity_is_equal_to_min_execution_quantity() {
            Order order = new Order(6, security, Side.BUY, 40, 40, 1000, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(40000);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_ice_order_quantity_is_equal_to_min_execution_quantity() {
            IcebergOrder order = new IcebergOrder(6, security, Side.BUY, 22, 22, 800, buyerBroker, buyerShareholder, 10);
            buyerBroker.increaseCreditBy(14600);
            return security.addNewOrder(order, matcher).getFirst();
        }

        
        public void add_two_buy_orders_with_same_price() {
            Order order1 = new Order(6, security, Side.BUY, 10, 0, 300, buyerBroker, 
                                     buyerShareholder, LocalDateTime.now().minusHours(1));
            Order order2 = new Order(7, security, Side.BUY, 10, 0, 300, buyerBroker, 
                                     buyerShareholder, LocalDateTime.now().minusHours(2));

            buyerBroker.increaseCreditBy(6000);
            security.addNewOrder(order1, matcher);
            security.addNewOrder(order2, matcher);
        }

        public void add_two_buy_ice_orders_with_same_price() {
            IcebergOrder order1 = new IcebergOrder(6, security, Side.BUY, 10, 0, 300, buyerBroker, 
                                                   buyerShareholder, LocalDateTime.now().plusHours(1), 10);
            IcebergOrder order2 = new IcebergOrder(7, security, Side.BUY, 10, 0, 300, buyerBroker, 
                                                   buyerShareholder, LocalDateTime.now().plusHours(2), 10);

            buyerBroker.increaseCreditBy(6000);
            security.addNewOrder(order1, matcher);
            security.addNewOrder(order2, matcher);
        }

        public MatchResult add_sell_order_causes_rollback_for_buy_orders_with_same_price() {
            this.add_two_buy_orders_with_same_price();
            Order order = new Order(9, security, Side.SELL, 300, 300, 0, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 300);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_order_causes_rollback_for_buy_ice_orders_with_same_price() {
            this.add_two_buy_ice_orders_with_same_price();
            Order order = new Order(9, security, Side.SELL, 300, 300, 0, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 300);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_sell_stop_limit_order_but_not_enough_position() {
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 10, 100, sellerBroker, sellerShareholder, 525);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public MatchResult add_buy_stop_limit_order_but_not_enough_credit() {
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 10, 100, buyerBroker, buyerShareholder, 575);
            return security.addNewOrder(order, matcher).getFirst();
        }

        public void add_three_stop_limit_order_both_buy_and_sell() {
            List<StopLimitOrder> orders = Arrays.asList(
                new StopLimitOrder(6, security, Side.SELL, 15, 400, sellerBroker, sellerShareholder, 500),
                new StopLimitOrder(7, security, Side.SELL, 15, 300, sellerBroker, sellerShareholder, 400),
                new StopLimitOrder(8, security, Side.SELL, 15, 200, sellerBroker, sellerShareholder, 300),
                new StopLimitOrder(6, security, Side.BUY,  15, 700, buyerBroker, buyerShareholder, 600),
                new StopLimitOrder(7, security, Side.BUY,  15, 800, buyerBroker, buyerShareholder, 700),
                new StopLimitOrder(8, security, Side.BUY,  15, 900, buyerBroker, buyerShareholder, 800)
            );
            sellerShareholder.incPosition(security, 45);
            buyerBroker.increaseCreditBy(36000);
            orders.forEach(order -> security.addNewOrder(order, matcher));
        }

        public List<MatchResult> new_sell_order_activate_all_sell_stop_limit_orders() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            Order order = new Order(9, security, Side.SELL, 45, 500, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 45);
            return security.addNewOrder(order, matcher);
        }

        public List<MatchResult> new_buy_order_activate_all_buy_stop_limit_orders() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            Order order = new Order(9, security, Side.BUY, 10, 600, buyerBroker, buyerShareholder);
            buyerBroker.increaseCreditBy(6000);
            return security.addNewOrder(order, matcher);
        }

        public List<MatchResult> new_sell_order_activate_one_sell_stop_limit_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            Order order = new Order(9, security, Side.SELL, 30, 500, sellerBroker, sellerShareholder);
            sellerShareholder.incPosition(security, 30);
            return security.addNewOrder(order, matcher);
        }

        public List<MatchResult> new_buy_order_activate_one_buy_stop_limit_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            Order order1 = new Order(10, security, Side.SELL, 10, 600, sellerBroker, sellerShareholder);
            Order order2 = new Order(9, security, Side.BUY, 5, 600, buyerBroker, buyerShareholder);
            sellerShareholder.incPosition(security, 10);
            buyerBroker.increaseCreditBy(3000);
            security.addNewOrder(order1, matcher);
            return security.addNewOrder(order2, matcher);
        }

        public List<MatchResult> new_sell_stop_limit_order_and_active_at_the_first() {
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 10, 500, sellerBroker, sellerShareholder, 600);
            sellerShareholder.incPosition(security, 10);
            return security.addNewOrder(order, matcher);
        }

        public List<MatchResult> new_buy_stop_limit_order_and_active_at_the_first() {
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 3, 700, buyerBroker, buyerShareholder, 500);
            buyerBroker.increaseCreditBy(2100);
            return security.addNewOrder(order, matcher);
        }

        public MatchResult decrease_price_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 15, 350, sellerBroker, sellerShareholder, 500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_price_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 15, 450, sellerBroker, sellerShareholder, 500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_quantity_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 10, 400, sellerBroker, sellerShareholder, 500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_quantity_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 20, 400, sellerBroker, sellerShareholder, 500);
            sellerShareholder.incPosition(security, 5);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_quantity_stop_limit_sell_order_and_not_enough_position() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 20, 400, sellerBroker, sellerShareholder, 500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_stop_price_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 15, 400, sellerBroker, sellerShareholder, 350);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_stop_price_stop_limit_sell_order_and_not_activated() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 15, 400, sellerBroker, sellerShareholder, 525);
            return security.updateOrder(order, matcher).getFirst();
        }

        public List<MatchResult> increase_stop_price_stop_limit_sell_order_and_activated() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.SELL, 15, 400, sellerBroker, sellerShareholder, 555);
            return security.updateOrder(order, matcher);
        }
        public MatchResult decrease_price_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 15, 600, buyerBroker, buyerShareholder, 600);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_price_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 15, 750, buyerBroker, buyerShareholder, 600);
            buyerBroker.increaseCreditBy(750);
            return security.updateOrder(order, matcher).getFirst();
        
        }
        public MatchResult increase_price_stop_limit_buy_order_and_not_enough_credit() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 15, 750, buyerBroker, buyerShareholder, 600);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_quantity_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 10, 700, buyerBroker, buyerShareholder, 600);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_quantity_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 20, 700, buyerBroker, buyerShareholder, 600);
            buyerBroker.increaseCreditBy(3500);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult increase_quantity_stop_limit_buy_order_and_not_enough_credit() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 20, 700, buyerBroker, buyerShareholder, 600);
            return security.updateOrder(order, matcher).getFirst();
        }

        public MatchResult decrease_stop_price_stop_limit_buy_order_and_not_activated() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(7, security, Side.BUY, 15, 800, buyerBroker, buyerShareholder, 575);
            return security.updateOrder(order, matcher).getFirst();
        }

        public List<MatchResult> decrease_stop_price_stop_limit_buy_order_and_activated() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 15, 700, buyerBroker, buyerShareholder, 500);
            return security.updateOrder(order, matcher);
        }

        public MatchResult increase_stop_price_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            StopLimitOrder order = new StopLimitOrder(6, security, Side.BUY, 15, 700, buyerBroker, buyerShareholder, 750);
            return security.updateOrder(order, matcher).getFirst();
        }
        
        public void delete_stop_limit_sell_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            security.deleteOrder(Side.SELL, 7);
        }
        
        public void delete_stop_limit_buy_order() {
            this.add_three_stop_limit_order_both_buy_and_sell();
            security.deleteOrder(Side.BUY, 7);
        }
    }


    // --------------------------------------------------------------------------------
    // Test cases
    // --------------------------------------------------------------------------------

    @BeforeEach
    void setup() {
        security = Security.builder().lastTradePrice(550).build();
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
        assertPack = new AssertingPack();
        scenarioGenerator = new ScenarioGenerator();
    }

    @Test
    public void delete_sell_order_and_check_buyer_credit() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void delete_sell_order_and_check_buyer_position() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_sell_order_and_check_seller_position() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_sell_order_and_check_seller_credit() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void delete_sell_order_and_check_sell_side_in_queue() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertOrderInQueue(Side.SELL, 1, 3, 10, 800);
    }

    @Test
    public void delete_sell_order_and_check_buy_side_in_queue() {
        scenarioGenerator.delete_sell_order();
        assertPack.assertOrderInQueue(Side.BUY, 3, 2, 10, 200);
    }
    
    @Test
    public void delete_sell_ice_order_and_check_buyer_credit() {
        scenarioGenerator.delete_sell_ice_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void delete_sell_ice_order_and_check_buyer_position() {
        scenarioGenerator.delete_sell_ice_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_sell_ice_order_and_check_seller_position() {
        scenarioGenerator.delete_sell_ice_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_sell_ice_order_and_check_seller_credit() {
        scenarioGenerator.delete_sell_ice_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void delete_sell_ice_order_and_check_buy_side_in_queue() {
        scenarioGenerator.delete_sell_ice_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void delete_sell_ice_order_and_check_sell_side_in_queue() {
        scenarioGenerator.delete_sell_ice_order();
        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> orderBook.getSellQueue().get(4));
    }

    @Test
    public void delete_buy_order_and_check_buyer_credit() {
        scenarioGenerator.delete_buy_order();
        assertPack.exceptedBuyerCredit = 3000;
        assertPack.assertBuyerCredit();
    }
    
    @Test
    public void delete_buy_order_and_check_buyer_position() {
        scenarioGenerator.delete_buy_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_buy_order_and_check_seller_credit() {
        scenarioGenerator.delete_buy_order();
        assertPack.assertSellerCredit();
    }
    
    @Test
    public void delete_buy_order_and_check_seller_position() {
        scenarioGenerator.delete_buy_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_buy_order_and_check_sell_side_in_queue() {
        scenarioGenerator.delete_buy_order();
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
    }

    @Test
    public void delete_buy_order_and_check_buy_side_in_queue() {
        scenarioGenerator.delete_buy_order();
        assertPack.assertOrderInQueue(Side.BUY, 2, 2, 10, 200);
    }

    @Test
    public void delete_buy_ice_order_and_check_buyer_credit() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.exceptedBuyerCredit = 22500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void delete_buy_ice_order_and_check_buyer_position() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_buy_ice_order_and_check_seller_credit() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void delete_buy_ice_order_and_check_seller_position() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_buy_ice_order_and_check_sell_side_in_queue() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    @Test
    public void delete_buy_ice_order_and_check_buy_side_in_queue() {
        scenarioGenerator.delete_buy_ice_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void delete_non_existing_sell_order_and_check_buyer_credit() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertBuyerCredit();
    }

    @Test
    public void delete_non_existing_sell_order_and_check_buyer_position() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_non_existing_sell_order_and_check_buy_side_in_queue() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
    }

    @Test
    public void delete_non_existing_sell_order_and_check_seller_credit() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertSellerCredit();
    }

    @Test
    public void delete_non_existing_sell_order_and_check_seller_position() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_non_existing_sell_order_and_check_sell_side_in_queue() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_sell_order());
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    @Test
    public void delete_non_existing_buy_order_and_check_buyer_credit() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertBuyerCredit();
    }

    @Test
    public void delete_non_existing_buy_order_and_check_buyer_position() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertBuyerPosition();
    }

    @Test
    public void delete_non_existing_buy_order_and_check_buy_side_in_queue() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
    }

    @Test
    public void delete_non_existing_buy_order_and_check_seller_credit() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertSellerCredit();
    }

    @Test
    public void delete_non_existing_buy_order_and_check_seller_position() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertSellerPosition();
    }

    @Test
    public void delete_non_existing_buy_order_and_check_sell_side_in_queue() {
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> scenarioGenerator.delete_non_existing_buy_order());
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    // TODO
    // what if new quantity be zero? what should happen in that case?

    @Test
    public void decrease_sell_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_sell_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_order_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_order_quantity_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_order_quantity_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_order_quantity_and_check_seller_position() {
        scenarioGenerator.decrease_sell_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.decrease_sell_order_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 4, 600);
    }

    @Test
    public void decrease_sell_ice_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_ice_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_sell_ice_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_ice_order_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_ice_order_quantity_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_ice_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_ice_order_quantity_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_ice_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_ice_order_quantity_and_check_seller_position() {
        scenarioGenerator.decrease_sell_ice_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_ice_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 30, 1000, 10, 10);
    }

    @Test
    public void decrease_buy_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_buy_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_buy_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.decrease_buy_order_quantity();
        assertPack.exceptedBuyerCredit = 900;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_buy_order_quantity_and_check_buyer_position() {
        scenarioGenerator.decrease_buy_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_buy_order_quantity_and_check_seller_credit() {
        scenarioGenerator.decrease_buy_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_buy_order_quantity_and_check_seller_position() {
        scenarioGenerator.decrease_buy_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_buy_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.decrease_buy_order_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 2, 3, 7, 300);
    }

    @Test
    public void decrease_buy_ice_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_buy_ice_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_buy_ice_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.decrease_buy_ice_order_quantity();
        assertPack.exceptedBuyerCredit = 19000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_buy_ice_order_quantity_and_check_buyer_position() {
        scenarioGenerator.decrease_buy_ice_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_buy_ice_order_quantity_and_check_seller_credit() {
        scenarioGenerator.decrease_buy_ice_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_buy_ice_order_quantity_and_check_seller_position() {
        scenarioGenerator.decrease_buy_ice_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_buy_ice_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.decrease_buy_ice_order_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 7, 500, 10, 7);
    }
    
    @Test
    public void increase_sell_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_sell_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_sell_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_order_quantity();
        assertPack.assertBuyerCredit();
    }
    
    @Test
    public void increase_sell_order_quantity_and_check_buyer_position() {
        scenarioGenerator.increase_sell_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_order_quantity_and_check_seller_credit() {
        scenarioGenerator.increase_sell_order_quantity();
        assertPack.assertSellerCredit();
    }
    
    @Test
    public void increase_sell_order_quantity_and_check_seller_position() {
        scenarioGenerator.increase_sell_order_quantity();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_order_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 15, 700);
    }

    @Test
    public void increase_sell_ice_order_quantity_and_check_matcu_result() {
        MatchResult res = scenarioGenerator.increase_sell_ice_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_sell_ice_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_ice_order_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_sell_ice_order_quantity_and_check_buyer_position() {
        scenarioGenerator.increase_sell_ice_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_ice_order_quantity_and_check_seller_credit() {
        scenarioGenerator.increase_sell_ice_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_sell_ice_order_quantity_and_check_seller_position() {
        scenarioGenerator.increase_sell_ice_order_quantity();
        assertPack.exceptedSellerPosition = 100;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_ice_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_ice_order_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 60, 1000, 10, 10);
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_buyer_position() {
        scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_seller_credit() {
        scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_seller_position() {
        scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_order_quantity_but_not_enough_position_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_order_quantity_but_not_enough_position();
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
    }

    @Test
   public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
   }

   @Test
   public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_buyer_position() {
        scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_seller_credit() {
        scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_seller_position() {
        scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_ice_order_quantity_but_not_enough_position_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_ice_order_quantity_but_not_enough_position();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    @Test
    public void increase_buy_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_quantity_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_quantity_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_quantity_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_order_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 1, 4, 25, 400);
    }

    @Test
    public void increase_buy_ice_order_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_ice_order_quantity_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_quantity_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_quantity();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_quantity_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_quantity();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_quantity_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_quantity_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_ice_order_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 60, 500, 10, 10);
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }
    
    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_quantity_but_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_order_quantity_but_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 1, 4, 10, 400);
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_quantity_but_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_ice_order_quantity_but_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    // TODO
    // add peakSize scenarios after you are sure how they work

    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertPack.assertBuyerCredit();
    }
    
    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_seller_position() {
        scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_order_price_no_trading_happens_and_check_order_in_queue() {
        scenarioGenerator.decrease_sell_order_price_no_trading_happens();
        assertPack.assertOrderInQueue(Side.SELL, 1, 3, 10, 650);
    }
    
    @Test
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_seller_position() {
        scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertPack.assertSellerPosition();
    }

    @Test   
    public void decrease_sell_ice_order_price_no_trading_happens_and_check_order_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_price_no_trading_happens();
        assertPack.assertOrderInQueue(Side.SELL, 1, 5, 45, 600, 10, 10);
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.exceptedBuyerPosition = 10;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.exceptedSellerCredit = 5000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.exceptedSellerPosition = 75;
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.SELL, 2, 4, 10, 900);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 3)).isFalse();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 35, 500, 10, 10);
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertPack.exceptedSellerPosition = 40;
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 5)).isFalse();
    }

    @Test
    public void decrease_sell_ice_order_price_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertPack.exceptedSellerPosition = 80;
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 3, 5, 450);
    }

    @Test
    public void decrease_sell_order_price_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.decrease_sell_order_price_and_partially_traded();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertPack.exceptedSellerPosition = 45;
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 5, 5, 450, 10, 5);
    }

    @Test
    public void decrease_sell_ice_order_price_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.decrease_sell_ice_order_price_and_partially_traded();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void decrease_buy_order_price_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_buy_order_price();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test
    public void decrease_buy_order_price_and_check_buyer_credit() {
        scenarioGenerator.decrease_buy_order_price();
        assertPack.exceptedBuyerCredit = 1500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_buy_order_price_and_check_buyer_position() {
        scenarioGenerator.decrease_buy_order_price();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_buy_order_price_and_check_seller_credit() {
        scenarioGenerator.decrease_buy_order_price();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_buy_order_price_and_check_seller_position() {
        scenarioGenerator.decrease_buy_order_price();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_buy_order_price_and_check_order_in_queue() {
        scenarioGenerator.decrease_buy_order_price();
        assertPack.assertOrderInQueue(Side.BUY, 3, 3, 10, 150);
    }

    @Test
    public void decrease_buy_ice_order_price_and_check_match_result() {
        MatchResult res = scenarioGenerator.decrease_buy_ice_order_price();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void decrease_buy_ice_order_price_and_check_buyer_credit() {
        scenarioGenerator.decrease_buy_ice_order_price();
        assertPack.exceptedBuyerCredit = 13500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void decrease_buy_ice_order_price_and_check_buyer_position() {
        scenarioGenerator.decrease_buy_ice_order_price();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void decrease_buy_ice_order_price_and_check_seller_credit() {
        scenarioGenerator.decrease_buy_ice_order_price();
        assertPack.assertSellerCredit();
    }

    @Test
    public void decrease_buy_ice_order_price_and_check_seller_position() {
        scenarioGenerator.decrease_buy_ice_order_price();
        assertPack.assertSellerPosition();
    }

    @Test
    public void decrease_buy_ice_order_price_and_check_order_in_queue() {
        scenarioGenerator.decrease_buy_ice_order_price();
        assertPack.assertOrderInQueue(Side.BUY, 3, 5, 45, 200, 10, 10);
    }

    @Test
    public void increase_sell_order_price_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_sell_order_price();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_sell_order_price_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_order_price();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_sell_order_price_and_check_buyer_position() {
        scenarioGenerator.increase_sell_order_price();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_order_price_and_check_seller_credit() {
        scenarioGenerator.increase_sell_order_price();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_sell_order_price_and_check_seller_position() {
        scenarioGenerator.increase_sell_order_price();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_order_price_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_order_price();
        assertPack.assertOrderInQueue(Side.SELL, 3, 3, 10, 950);
    }

    @Test
    public void increase_sell_ice_order_price_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_sell_ice_order_price();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_sell_ice_order_price_and_check_buyer_credit() {
        scenarioGenerator.increase_sell_ice_order_price();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_sell_ice_order_price_and_check_buyer_position() {
        scenarioGenerator.increase_sell_ice_order_price();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_sell_ice_order_price_and_check_seller_credit() {
        scenarioGenerator.increase_sell_ice_order_price();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_sell_ice_order_price_and_check_seller_position() {
        scenarioGenerator.increase_sell_ice_order_price();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_sell_ice_order_price_and_check_order_in_queue() {
        scenarioGenerator.increase_sell_ice_order_price();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1100, 10, 10);
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens();
        assertPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 250);
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 550, 10, 10);
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_price_no_trading_happens_and_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 4, 1, 10, 100);
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_no_trading_happens_and_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertPack.exceptedBuyerCredit = 1000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertPack.exceptedBuyerPosition = 10;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertPack.exceptedSellerCredit = 6000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertPack.exceptedSellerPosition = 75;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 1)).isFalse();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    @Test
    public void increase_buy_order_price_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 100);
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.exceptedSellerCredit = 35000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.exceptedSellerPosition = 40;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 5, 40, 1000, 10, 5);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 5)).isTrue();
    }

    @Test
    public void increase_buy_ice_order_price_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
    }
    
    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.exceptedBuyerPosition = 20;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.exceptedSellerCredit = 13000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.exceptedSellerPosition = 65;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 1)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 2)).isFalse();
        assertPack.assertOrderInQueue(Side.SELL, 0, 3, 10, 800);
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 3, 5, 700);
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertPack.exceptedBuyerCredit = 22500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertPack.exceptedSellerCredit = 75000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertPack.exceptedSellerPosition = 0;
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertThat(orderBook.getSellQueue().size()).isZero();
    }

    @Test
    public void increase_buy_ice_order_price_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_partially_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 5, 1000, 10, 5);
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertPack.exceptedBuyerCredit = 13500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 1)).isTrue();
    }

    @Test
    public void increase_buy_order_price_and_trade_happens_but_not_enough_credit_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_order_price_and_trade_happens_but_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500);
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_bueyr_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.exceptedBuyerCredit = 57000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_buyer_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_seller_credit() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.assertSellerCredit();
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_seller_position() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.assertSellerPosition();
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        assertPack.assertOrderInQueue(Side.SELL, 2, 3, 10, 800);
        assertPack.assertOrderInQueue(Side.SELL, 3, 4, 10, 900);
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
    }

    @Test
    public void increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.increase_buy_ice_order_price_and_trade_happens_but_not_enough_credit_causes_rollback();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500);
    }

    // TODO
    // add some test about updating a ice order that its display is not equal to its quantity
    
    public void add_sell_order_no_trades_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_no_trades_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_order_no_trades_happens_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_no_trades_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_no_trades_happens_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_no_trades_happens();
        assertPack.assertBuyerPosition();
    }
    
    @Test
    public void add_sell_order_no_trades_happens_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_no_trades_happens();
        assertPack.assertSellerCredit();
    }
    
    @Test
    public void add_sell_order_no_trades_happens_and_check_seller_position() {
        scenarioGenerator.add_sell_order_no_trades_happens();
        assertPack.exceptedSellerPosition = 100;
        assertPack.assertSellerPosition();
    }
    
    @Test
    public void add_sell_order_no_trades_happens_and_check_order_in_queue() {
        scenarioGenerator.add_sell_order_no_trades_happens();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 1, 6, 15, 650);
        assertPack.assertOrderInQueue(Side.SELL, 2, 2, 10, 700);
    }
    
    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertPack.exceptedSellerPosition = 105;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_no_trades_happens_and_check_order_in_queue() {
        scenarioGenerator.add_sell_ice_order_no_trades_happens();
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000, 10, 10);
        assertPack.assertOrderInQueue(Side.SELL, 5, 6, 20, 1000, 7, 7);
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_and_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_and_not_enough_position();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_and_not_enough_position();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_and_not_enough_position();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_seller_position() {
        scenarioGenerator.add_sell_order_and_not_enough_position();
        assertPack.exceptedSellerPosition = 85;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_and_not_enough_position_and_check_order_in_queue() {
        scenarioGenerator.add_sell_order_and_not_enough_position();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_and_not_enough_position_and_check_order_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_not_enough_position();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6));
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_order_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertPack.exceptedBuyerPosition = 13;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertPack.exceptedSellerCredit = 6500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertPack.exceptedSellerPosition = 85;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 8)).isFalse();
    }

    @Test
    public void add_sell_order_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 32, 500, 10, 7);
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertPack.exceptedBuyerPosition = 67;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertPack.exceptedSellerCredit = 29900;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 8)).isFalse();
    }

    @Test
    public void add_sell_ice_order_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 2, 8, 200);
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_order_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertPack.exceptedSellerPosition = 100;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 7, 15, 500);
    }

    @Test
    public void add_sell_order_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_and_partially_traded();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedBuyerPosition = 55;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedSellerCredit = 26500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.assertOrderInQueue(Side.SELL, 0, 7, 5, 400, 3, 3);
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 4)).isFalse();
        assertPack.assertOrderInQueue(Side.BUY, 0, 3, 10, 300);
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedBuyerPosition = 55;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedSellerCredit = 26500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.assertOrderInQueue(Side.SELL, 0, 7, 5, 400, 7, 5);
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 5)).isFalse();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 4)).isFalse();
        assertPack.assertOrderInQueue(Side.BUY, 0, 3, 10, 300);
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertPack.exceptedSellerCredit = 32500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_seller_position() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertPack.exceptedSellerPosition = 85;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_finished();
        assertThat(orderBook.getBuyQueue().size()).isZero();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertPack.exceptedSellerCredit = 32500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_finished();
        assertThat(orderBook.getBuyQueue().size()).isZero();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedSellerCredit = 32500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_seller_position() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedSellerPosition = 120;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.assertOrderInQueue(Side.SELL, 0, 6, 35, 100);
    }

    @Test
    public void add_sell_order_matches_with_all_buyer_queue_and_not_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_matches_with_all_buyer_queue_and_not_finished();
        assertThat(orderBook.getBuyQueue().size()).isZero();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedSellerCredit = 32500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.exceptedSellerPosition = 100;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertPack.assertOrderInQueue(Side.SELL, 0, 6, 15, 100, 10, 10);
    }

    @Test
    public void add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_matches_with_all_buyer_queue_and_not_finished();
        assertThat(orderBook.getBuyQueue().size()).isZero();
    }

    @Test 
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test 
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_position() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.SELL, 0, 6, 5, 10, 500);
    }
    
    @Test
    public void add_sell_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test 
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test 
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedBuyerPosition = 45;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerCredit = 22500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.SELL, 0, 6, 5, 10, 500, 10, 5);
    }

    @Test
    public void add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test 
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
    }    
    
    @Test 
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerCredit();
    }
    
    @Test
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_seller_position() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.exceptedSellerPosition = 145;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_order_not_enough_execution_cause_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test 
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
    }
    
    @Test 
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.exceptedSellerPosition = 185;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
    }

    @Test
    public void add_sell_ice_order_not_enough_execution_cause_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertPack.assertOrderInQueue(Side.BUY, 1, 4, 10, 400);
        assertPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test 
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test 
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_credit() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_position() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedBuyerPosition = 50;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_credit() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerCredit = 24500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_position() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_order_quantity_is_equal_to_min_execution_quantity_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 5, 400);
    }

    @Test 
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test 
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_credit() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_position() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedBuyerPosition = 50;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_credit() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerCredit = 24500;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_position() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_sell_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertThat(orderBook.isThereOrderWithId(Side.SELL, 6)).isFalse();
    }

    @Test
    public void add_sell_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buy_side_in_queue() {
        scenarioGenerator.add_sell_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 5, 400);
    }

    @Test
    public void add_buy_order_no_trades_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_no_trades_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_no_trades_happens_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_no_trades_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_no_trades_happens_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_no_trades_happens();
        assertPack.exceptedBuyerPosition = 0;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_no_trades_happens_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_no_trades_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_no_trades_happens_and_check_seller_position() {
        scenarioGenerator.add_buy_order_no_trades_happens();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_no_trades_happens_and_check_order_in_queue() {
        scenarioGenerator.add_buy_order_no_trades_happens();
        assertPack.assertOrderInQueue(Side.BUY, 4, 2, 10, 200);
        assertPack.assertOrderInQueue(Side.BUY, 3, 6, 22, 300);
        assertPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_no_trades_happens_and_check_order_in_queue() {
        scenarioGenerator.add_buy_ice_order_no_trades_happens();
        assertPack.assertOrderInQueue(Side.BUY, 2, 4, 10, 400);
        assertPack.assertOrderInQueue(Side.BUY, 1, 6, 5, 450, 1, 1);
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void add_buy_order_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }
    
    @Test
    public void add_buy_order_but_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertPack.exceptedBuyerCredit = 6000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_but_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_but_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_but_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_but_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.add_buy_order_but_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 3, 2, 10, 200);
        assertPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }
    
    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertPack.exceptedBuyerCredit = 2000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_but_not_enough_credit_and_check_order_in_queue() {
        scenarioGenerator.add_buy_ice_order_but_not_enough_credit();
        assertPack.assertOrderInQueue(Side.BUY, 1, 4, 10, 400);
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.exceptedBuyerPosition = 13;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.exceptedSellerCredit = 8100;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.exceptedSellerPosition = 72;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 7, 700);
    }

    @Test
    public void add_buy_order_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 8)).isFalse();
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_matc_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.exceptedBuyerPosition = 52;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.exceptedSellerCredit = 42000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.exceptedSellerPosition = 33;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 5, 33, 1000, 10, 8);
    }

    @Test
    public void add_buy_ice_order_and_completely_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_completely_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 8)).isFalse();
    }
    
    public void add_buy_order_and_partially_traded_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_buy_order_and_partially_traded();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    public void add_buy_order_and_partially_traded_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.assertBuyerCredit();
    }

    public void add_buy_order_and_partially_traded_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.exceptedBuyerPosition = 10;
        assertPack.assertBuyerPosition();
    }

    public void add_buy_order_and_partially_traded_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.exceptedSellerCredit = 6000;
        assertPack.assertSellerCredit();
    }

    public void add_buy_order_and_partially_traded_and_check_seller_position() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.exceptedSellerPosition = 75;
        assertPack.assertSellerPosition();
    }

    public void add_buy_order_and_partially_traded_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    public void add_buy_order_and_partially_traded_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_and_partially_traded();
        assertPack.assertOrderInQueue(Side.BUY, 0, 6, 3, 600);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.assertBuyerCredit();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedBuyerPosition = 10;
        assertPack.assertBuyerPosition();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedSellerCredit = 6000;
        assertPack.assertSellerCredit();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedSellerPosition = 75;
        assertPack.assertSellerPosition();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.assertOrderInQueue(Side.BUY, 0, 6, 3, 600, 2, 2);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.assertBuyerCredit();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedBuyerPosition = 10;
        assertPack.assertBuyerPosition();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedSellerCredit = 6000;
        assertPack.assertSellerCredit();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.exceptedSellerPosition = 75;
        assertPack.assertSellerPosition();
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    public void add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_and_partially_traded_and_remainder_is_less_than_peak_size();
        assertPack.assertOrderInQueue(Side.BUY, 0, 6, 4, 600, 5, 4);
    }
    
    public void add_buy_order_not_enough_credit_causes_rollback_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertPack.exceptedBuyerCredit = 9000;
        assertPack.assertBuyerCredit();
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertPack.assertBuyerPosition();
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertPack.assertSellerCredit();
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_seller_position() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertPack.assertSellerPosition();
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
    }

    public void add_buy_order_not_enough_credit_causes_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_not_enough_credit_causes_rollback();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_match_check() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertPack.exceptedBuyerCredit = 78000;
        assertPack.assertBuyerCredit();
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertPack.assertBuyerPosition();
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertPack.assertSellerCredit();
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertPack.assertSellerPosition();
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        assertPack.assertOrderInQueue(Side.SELL, 2, 3, 10, 800);
        assertPack.assertOrderInQueue(Side.SELL, 3, 4, 10, 900);
        assertPack.assertOrderInQueue(Side.SELL, 4, 5, 45, 1000);
    }

    public void add_buy_ice_order_not_enough_credit_causes_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_not_enough_credit_causes_rollback();
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedSellerCredit = 75000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_seller_position() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedSellerPosition = 0;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertThat(orderBook.getSellQueue().size()).isZero();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_finished();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedSellerCredit = 75000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertPack.exceptedSellerPosition = 0;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertThat(orderBook.getSellQueue().size()).isZero();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_finished();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedSellerCredit = 75000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_seller_position() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedSellerPosition = 0;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertThat(orderBook.getSellQueue().size()).isZero();
    }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.assertOrderInQueue(Side.BUY, 0, 8, 15, 1000);
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedBuyerPosition = 85;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedSellerCredit = 75000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedSellerPosition = 0;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertThat(orderBook.getSellQueue().size()).isZero();
    }

    @Test
    public void add_buy_ice_order_matches_with_all_seller_queue_and_not_finished_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.assertOrderInQueue(Side.BUY, 0, 8, 15, 1000, 10, 10);
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedBuyerPosition = 20;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerCredit = 13000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_position() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerPosition = 65;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.SELL, 0, 3, 10, 800);
    }

    @Test
    public void add_buy_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.BUY, 0, 6, 2, 17, 700);
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedBuyerPosition = 20;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerCredit = 13000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.exceptedSellerPosition = 65;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.SELL, 0, 3, 10, 800);
    }

    @Test
    public void add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_with_min_execution_quantity_and_next_go_to_queue();
        assertPack.assertOrderInQueue(Side.BUY, 0, 6, 12, 20, 700, 10, 10);
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
    }
    
    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.exceptedBuyerCredit = 36000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_seller_position() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
    }

    @Test
    public void add_buy_order_not_enough_execution_cause_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
    }
    
    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.exceptedBuyerCredit = 80000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 1, 2, 10, 700);
        assertPack.assertOrderInQueue(Side.SELL, 2, 3, 10, 800);    }

    @Test
    public void add_buy_ice_order_not_enough_execution_cause_rollback_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_not_enough_execution_cause_rollback();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }
    
    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_credit() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedBuyerCredit = 10000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_position() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedBuyerPosition = 40;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_credit() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerCredit = 30000;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_position() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerPosition = 45;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 0, 5, 45, 1000, 10, 10);
    }

    @Test
    public void add_buy_order_quantity_is_equal_to_min_execution_quantity_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    @Test 
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.EXECUTED);
    }

    @Test 
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_credit() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buyer_position() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedBuyerPosition = 22;
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_credit() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerCredit = 14600;
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_seller_position() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.exceptedSellerPosition = 63;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_sell_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.SELL, 0, 3, 8, 800);
    }

    @Test
    public void add_buy_ice_order_quantity_is_equal_to_min_execution_quantity_and_check_buy_side_in_queue() {
        scenarioGenerator.add_buy_ice_order_quantity_is_equal_to_min_execution_quantity();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertThat(orderBook.isThereOrderWithId(Side.BUY, 6)).isFalse();
    }

    // FIXME: These three have race condition problem
    // @Test
    // public void add_two_buy_orders_with_same_price_and_check_orders_in_queue() {
    //     scenarioGenerator.add_two_buy_orders_with_same_price();
    //     assertPack.assertOrderInQueue(Side.BUY, 2, 7, 10, 300);
    //     assertPack.assertOrderInQueue(Side.BUY, 3, 6, 10, 300);
    //     assertPack.assertOrderInQueue(Side.BUY, 4, 3, 10, 300);
    // }

    // @Test
    // public void add_two_buy_ice_orders_with_same_price_and_check_orders_in_queue() {
        //     scenarioGenerator.add_two_buy_ice_orders_with_same_price();
        //     assertPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
        //     assertPack.assertOrderInQueue(Side.BUY, 3, 6, 10, 300, 10, 10);
        //     assertPack.assertOrderInQueue(Side.BUY, 4, 7, 10, 300, 10, 10);
    // }

    // @Test
    // public void add_sell_order_causes_rollback_for_buy_orders_with_same_price_and_check_match_result() {
        //     MatchResult res = scenarioGenerator.add_sell_order_causes_rollback_for_buy_orders_with_same_price();
        //     assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
        // }
        
    @Test
    public void add_sell_order_causes_rollback_for_buy_orders_with_same_price_and_check_orders_in_queue() {
        scenarioGenerator.add_sell_order_causes_rollback_for_buy_orders_with_same_price();
        assertPack.assertOrderInQueue(Side.BUY, 2, 7, 10, 300);
        assertPack.assertOrderInQueue(Side.BUY, 3, 6, 10, 300);
        assertPack.assertOrderInQueue(Side.BUY, 4, 3, 10, 300);
    }
    
    @Test
    public void add_sell_order_causes_rollback_for_buy_ice_orders_with_same_price_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_order_causes_rollback_for_buy_ice_orders_with_same_price();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_EXECUTION);
    }
    
    // FIXME: This has race condition problem
    // @Test
    // public void add_sell_order_causes_rollback_for_buy_ice_orders_with_same_price_and_check_orders_in_queue() {
    //     scenarioGenerator.add_sell_order_causes_rollback_for_buy_ice_orders_with_same_price();
    //     assertPack.assertOrderInQueue(Side.BUY, 2, 3, 10, 300);
    //     assertPack.assertOrderInQueue(Side.BUY, 3, 6, 10, 300, 10, 10);
    //     assertPack.assertOrderInQueue(Side.BUY, 4, 7, 10, 300, 10, 10);
    // }

    @Test
    public void add_buy_order_matches_with_all_seller_queue_and_not_finished_and_check_last_trade_price() {
        scenarioGenerator.add_buy_order_matches_with_all_seller_queue_and_not_finished();
        assertPack.exceptedLastTradePrice = 1000;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size_and_check_last_trade_price() {
        scenarioGenerator.add_sell_ice_order_and_partially_traded_and_remainder_is_bigger_than_peak_size();
        assertPack.exceptedLastTradePrice = 400;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void decrease_sell_order_price_and_completely_traded_and_check_last_trade_price() {
        scenarioGenerator.decrease_sell_order_price_and_completely_traded();
        assertPack.exceptedLastTradePrice = 500;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void increase_buy_order_price_and_partially_traded_and_check_last_trade_price() {
        scenarioGenerator.increase_buy_order_price_and_partially_traded();
        assertPack.exceptedLastTradePrice = 700;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void new_sell_stop_limit_order_and_active_at_the_first_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.new_sell_stop_limit_order_and_active_at_the_first();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 6, 0);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 1);
    }

    @Test
    public void new_sell_stop_limit_order_and_active_at_the_first_and_check_last_trade_price() {
        scenarioGenerator.new_sell_stop_limit_order_and_active_at_the_first();
        assertPack.exceptedLastTradePrice = 500;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void new_buy_stop_limit_order_and_active_at_the_first_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.new_buy_stop_limit_order_and_active_at_the_first();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 6, 0);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 1);
    }

    @Test
    public void new_buy_stop_limit_order_and_active_at_the_first_and_check_last_trade_price() {
        scenarioGenerator.new_buy_stop_limit_order_and_active_at_the_first();
        assertPack.exceptedLastTradePrice = 600;
        assertPack.assertLastTradePrice();
    }

    @Test
    public void add_sell_stop_limit_order_but_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_sell_stop_limit_order_but_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
    }

    @Test
    public void add_buy_stop_limit_order_but_not_enough_credit_and_check_match_result() {
        MatchResult res = scenarioGenerator.add_buy_stop_limit_order_but_not_enough_credit();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_buyer_credit() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_buyer_position() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_seller_credit() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertSellerCredit();
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_seller_position() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.exceptedSellerPosition = 130;
        assertPack.assertSellerPosition();
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_last_trade_price() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertLastTradePrice();
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_stop_limit_sell_queue() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 400, 500);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 7, 15, 300, 400);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 2, 8, 15, 200, 300);
    }

    @Test
    public void add_three_stop_limit_order_both_buy_and_sell_and_check_stop_limit_buy_queue() {
        scenarioGenerator.add_three_stop_limit_order_both_buy_and_sell();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 15, 700, 600);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 7, 15, 800, 700);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 2, 8, 15, 900, 800);
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 9, 5);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 1);
        assertPack.assertMatchResult(results.get(2), MatchingOutcome.EXECUTED, 7, 1);
        assertPack.assertMatchResult(results.get(3), MatchingOutcome.EXECUTED, 8, 1);
    }
    
    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_buyer_credit() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.assertBuyerCredit();
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_buyer_position() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.exceptedBuyerPosition = 75;
        assertPack.assertBuyerPosition();
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_seller_credit() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.exceptedSellerCredit = 31500;
        assertPack.assertSellerCredit();
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_seller_position() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.exceptedSellerPosition = 100;
        assertPack.assertSellerPosition();
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_sell_queue() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.assertOrderInQueue(Side.SELL, 0, 8, 5, 200);
        assertPack.assertOrderInQueue(Side.SELL, 1, 7, 5, 300);
        assertPack.assertOrderInQueue(Side.SELL, 2, 6, 5, 400);
        assertPack.assertOrderInQueue(Side.SELL, 3, 1, 10, 600);
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_buy_queue() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.assertOrderInQueue(Side.BUY, 0, 1, 10, 100);
        assertThat(orderBook.getBuyQueue().size()).isEqualTo(1);
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_stop_limit_sell_queue() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertThat(orderBook.getStopLimitOrderSellQueue().isEmpty()).isTrue();
    }

    @Test 
    public void new_sell_order_activate_all_sell_stop_limit_orders_and_check_stop_limit_buy_queue() {
        scenarioGenerator.new_sell_order_activate_all_sell_stop_limit_orders();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 15, 700, 600);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 7, 15, 800, 700);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 2, 8, 15, 900, 800);
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 9, 1);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 1);
        assertPack.assertMatchResult(results.get(2), MatchingOutcome.EXECUTED, 7, 1);
        assertPack.assertMatchResult(results.get(3), MatchingOutcome.EXECUTED, 8, 1);
    }
    
    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_buyer_credit() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.assertBuyerCredit();
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_buyer_position() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.exceptedBuyerPosition = 40;
        assertPack.assertBuyerPosition();
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_seller_credit() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.exceptedSellerCredit = 30000;
        assertPack.assertSellerCredit();
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_seller_position() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.exceptedSellerPosition = 90;
        assertPack.assertSellerPosition();
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_sell_queue() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.assertOrderInQueue(Side.SELL, 0, 5, 45, 1000, 10, 10);
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_buy_queue() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.assertOrderInQueue(Side.BUY, 0, 8, 5, 900);
        assertPack.assertOrderInQueue(Side.BUY, 1, 7, 5, 800);
        assertPack.assertOrderInQueue(Side.BUY, 2, 6, 5, 700);
        assertPack.assertOrderInQueue(Side.BUY, 3, 5, 45, 500, 10, 10);        
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_stop_limit_sell_queue() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 400, 500);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 7, 15, 300, 400);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 2, 8, 15, 200, 300);
    }

    @Test 
    public void new_buy_order_activate_all_buy_stop_limit_orders_and_check_stop_limit_buy_queue() {
        scenarioGenerator.new_buy_order_activate_all_buy_stop_limit_orders();
        assertThat(orderBook.getStopLimitOrderBuyQueue().isEmpty()).isTrue(); 
    }

    @Test
    public void new_sell_order_activate_one_sell_stop_limit_order_and_check_buy_queue() {
        scenarioGenerator.new_sell_order_activate_one_sell_stop_limit_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
    }

    @Test
    public void new_sell_order_activate_one_sell_stop_limit_order_and_check_stop_limit_sell_queue() {
        scenarioGenerator.new_sell_order_activate_one_sell_stop_limit_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 7, 15, 300, 400);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 8, 15, 200, 300);
    }

    @Test
    public void new_buy_order_activate_one_buy_stop_limit_order_and_check_sell_queue() {
        scenarioGenerator.new_buy_order_activate_one_buy_stop_limit_order();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
    }

    @Test
    public void new_buy_order_activate_one_buy_stop_limit_order_and_check_stop_limit_buy_queue() {
        scenarioGenerator.new_buy_order_activate_one_buy_stop_limit_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 7, 15, 800, 700);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 8, 15, 900, 800);
    }

    @Test
    public void decrease_price_stop_limit_sell_order_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.decrease_price_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 350, 500);
    }

    @Test
    public void increase_price_stop_limit_sell_order_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.increase_price_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 450, 500);
    }

    @Test
    public void decrease_quantity_stop_limit_sell_order_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.decrease_quantity_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 10, 400, 500);
    }

    @Test
    public void increase_quantity_stop_limit_sell_order_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.increase_quantity_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 20, 400, 500);
    }

    @Test
    public void increase_quantity_stop_limit_sell_order_and_not_enough_position_and_check_match_result() {
        MatchResult res = scenarioGenerator.increase_quantity_stop_limit_sell_order_and_not_enough_position();
        assertThat(res.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_POSITIONS);
    }

    @Test
    public void decrease_stop_price_stop_limit_sell_order_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.decrease_stop_price_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 7, 15, 300, 400);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 6, 15, 400, 350);
    }

    @Test
    public void increase_stop_price_stop_limit_sell_order_and_not_activated_and_check_order_in_stop_limit_sell_queue() {
        scenarioGenerator.increase_stop_price_stop_limit_sell_order_and_not_activated();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 400, 525);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 7, 15, 300, 400);
    }

    @Test
    public void increase_stop_price_stop_limit_sell_order_and_activated_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.increase_stop_price_stop_limit_sell_order_and_activated();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 6, 0);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 2);
    }

    @Test
    public void increase_stop_price_stop_limit_sell_order_and_activated_and_check_stop_limit_sell_queue() {
        scenarioGenerator.increase_stop_price_stop_limit_sell_order_and_activated();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 7, 15, 300, 400);
    }

    @Test
    public void increase_stop_price_stop_limit_sell_order_and_activated_and_check_buy_queue() {
        scenarioGenerator.increase_stop_price_stop_limit_sell_order_and_activated();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 30, 500, 10, 5);
    }

    @Test
    public void decrease_price_stop_limit_buy_order_and_check_order_in_stop_limit_buy_queue() {
        scenarioGenerator.decrease_price_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 15, 600, 600);
    }

    @Test
    public void decrease_price_stop_limit_buy_order_and_check_buyer_credit() {
        scenarioGenerator.decrease_price_stop_limit_buy_order();
        assertPack.exceptedBuyerCredit = 1500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_price_stop_limit_buy_order_and_check_order_in_stop_limit_buy_queue() {
        scenarioGenerator.increase_price_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 15, 750, 600);
    }

    @Test
    public void increase_price_stop_limit_buy_order_and_check_buyer_credit() {
        scenarioGenerator.increase_price_stop_limit_buy_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_price_stop_limit_buy_order_and_not_enough_creadit() {
        MatchResult result = scenarioGenerator.increase_price_stop_limit_buy_order_and_not_enough_credit();
        assertThat(result.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void decrease_quantity_stop_limit_buy_order_and_check_order_in_stop_limit_buy_queue() {
        scenarioGenerator.decrease_quantity_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 10, 700, 600);
    }

    @Test
    public void decrease_quantity_stop_limit_buy_order_and_check_buyer_credit() {
        scenarioGenerator.decrease_quantity_stop_limit_buy_order();
        assertPack.exceptedBuyerCredit = 3500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_quantity_stop_limit_buy_order_and_check_in_stop_limit_buy_queue() {
        scenarioGenerator.increase_quantity_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 20, 700, 600);
    }

    @Test
    public void increase_quantity_stop_limit_buy_order_and_check_buyer_credit() {
        scenarioGenerator.increase_quantity_stop_limit_buy_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void increase_quantity_stop_limit_buy_order_and_not_enough_credit_and_check_match_result() {
        MatchResult result = scenarioGenerator.increase_quantity_stop_limit_buy_order_and_not_enough_credit();
        assertThat(result.outcome()).isEqualTo(MatchingOutcome.NOT_ENOUGH_CREDIT);
    }

    @Test
    public void decrease_stop_price_stop_limit_buy_order_and_not_activated_and_check_in_stop_limit_buy_order() {
        scenarioGenerator.decrease_stop_price_stop_limit_buy_order_and_not_activated();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 7, 15, 800, 575);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 6, 15, 700, 600);
    }

    @Test
    public void decrease_stop_price_stop_limit_buy_order_and_activated_and_check_match_results() {
        List<MatchResult> results = scenarioGenerator.decrease_stop_price_stop_limit_buy_order_and_activated();
        assertPack.assertMatchResult(results.get(0), MatchingOutcome.EXECUTED, 6, 0);
        assertPack.assertMatchResult(results.get(1), MatchingOutcome.EXECUTED, 6, 2);
        assertPack.assertMatchResult(results.get(2), MatchingOutcome.EXECUTED, 7, 2);
        assertPack.assertMatchResult(results.get(3), MatchingOutcome.EXECUTED, 8, 1);
    }

    @Test
    public void increase_stop_price_stop_limit_buy_order_and_check_order_in_stop_order_buy_queue() {
        scenarioGenerator.increase_stop_price_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 7, 15, 800, 700);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 6, 15, 700, 750);
    }

    @Test
    public void delete_stop_limit_sell_order_and_check_order_in_stop_order_sell_queue() {
        scenarioGenerator.delete_stop_limit_sell_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 6, 15, 400, 500);
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 1, 8, 15, 200, 300);
    }
    
    @Test
    public void delete_stop_limit_buy_order_and_check_order_in_stop_order_buy_queue() {
        scenarioGenerator.delete_stop_limit_buy_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 6, 15, 700, 600);
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 1, 8, 15, 900, 800);
    }

    @Test
    public void delete_stop_limit_buy_order_and_check_order_buyer_credit() {
        scenarioGenerator.delete_stop_limit_buy_order();
        assertPack.exceptedBuyerCredit = 12000;
        assertPack.assertBuyerCredit();
    }
}
