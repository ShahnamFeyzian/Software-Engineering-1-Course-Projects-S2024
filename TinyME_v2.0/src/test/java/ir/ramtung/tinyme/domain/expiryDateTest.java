package ir.ramtung.tinyme.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.domain.entity.IcebergOrder;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.OrderStatus;
import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.domain.entity.SecurityState;
import ir.ramtung.tinyme.domain.entity.Shareholder;
import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.domain.entity.StopLimitOrder;
import ir.ramtung.tinyme.domain.entity.Trade;
import ir.ramtung.tinyme.domain.entity.stats.AuctionStats;
import ir.ramtung.tinyme.domain.entity.stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.stats.SituationalStats;
import ir.ramtung.tinyme.domain.entity.stats.SituationalStatsType;
import ir.ramtung.tinyme.domain.entity.stats.StateStats;
import ir.ramtung.tinyme.domain.service.ExpiringService;
import ir.ramtung.tinyme.domain.service.ScheduleexpiryDate;

@SpringBootTest
public class expiryDateTest {
    private Security security;
	private Broker sellerBroker;
	private Broker buyerBroker;
	private Shareholder sellerShareholder;
	private Shareholder buyerShareholder;
	private OrderBook orderBook;
	private List<Order> orders;
	LocalDateTime entryTime = LocalDateTime.now();

    AssertingPack assertPack;
    ScenarioGenerator scenarioGenerator;

    @Mock
    Timer mockedTimer;

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
			exceptedSellerCredit = expiryDateTest.this.sellerBroker.getCredit();
			exceptedBuyerCredit = expiryDateTest.this.buyerBroker.getCredit();
			exceptedSellerPosition = expiryDateTest.this.sellerShareholder.getPositionBySecurity(security);
			exceptedBuyerPosition = expiryDateTest.this.buyerShareholder.getPositionBySecurity(security);
			exceptedLastTradePrice = expiryDateTest.this.security.getLastTradePrice();
			sellQueue = expiryDateTest.this.orderBook.getSellQueue();
			buyQueue = expiryDateTest.this.orderBook.getBuyQueue();
			sellStopLimitQueue = expiryDateTest.this.orderBook.getStopLimitOrderSellQueue();
			buyStopLimitQueue = expiryDateTest.this.orderBook.getStopLimitOrderBuyQueue();
		}

		private void assertSellerCredit() {
			assertThat(expiryDateTest.this.sellerBroker.getCredit()).isEqualTo(exceptedSellerCredit);
		}

		private void assertBuyerCredit() {
			assertThat(expiryDateTest.this.buyerBroker.getCredit()).isEqualTo(exceptedBuyerCredit);
		}

		private void assertSellerPosition() {
			assertThat(expiryDateTest.this.sellerShareholder.getPositionBySecurity(security))
				.isEqualTo(exceptedSellerPosition);
		}

		private void assertBuyerPosition() {
			assertThat(expiryDateTest.this.buyerShareholder.getPositionBySecurity(security))
				.isEqualTo(exceptedBuyerPosition);
		}

		private void assertLastTradePrice() {
			assertThat(expiryDateTest.this.security.getLastTradePrice()).isEqualTo(exceptedLastTradePrice);
		}

		private void assertOrderInStopLimitQueue(
			Side side,
			int idx,
			long orderId,
			int quantity,
			int price,
			int stopPrice
		) {
			StopLimitOrder order = (StopLimitOrder) (
				(side == Side.BUY) ? buyStopLimitQueue.get(idx) : sellStopLimitQueue.get(idx)
			);
			long actualId = order.getOrderId();
			int actualQuantity = order.getTotalQuantity();
			int actualPrice = order.getPrice();
			int actualStopPrice = order.getStopPrice();

			assertThat(actualId).isEqualTo(orderId);
			assertThat(actualQuantity).isEqualTo(quantity);
			assertThat(actualPrice).isEqualTo(price);
			assertThat(actualStopPrice).isEqualTo(stopPrice);
		}

		private void assertAuctionExecuteStats(ExecuteStats stats, int numOfTrades) {
			int actualNumOfTrades = stats.getTrades().size();
			assertThat(actualNumOfTrades).isEqualTo(numOfTrades);
		}

		private void assertContinuousExecuteStats(ExecuteStats stats, long orderId, int numOfTrades) {
			long actualOrderId = stats.getOrderId();
			assertThat(actualOrderId).isEqualTo(orderId);
			assertAuctionExecuteStats(stats, numOfTrades);	
		}

		private void assertAuctionStats(AuctionStats stats, int openingPrice, int tradableQuantity) {
			int actualOpeningPrice = stats.getOpeningPrice();
			int actualTradableQuantity = stats.getTradableQuantity();

			assertThat(actualOpeningPrice).isEqualTo(openingPrice);
			assertThat(actualTradableQuantity).isEqualTo(tradableQuantity);
		}

		private void assertSituationalStats(SituationalStats stats, SituationalStatsType type, long orderId) {
			SituationalStatsType actualType = stats.getType();
			long actualOrderId = stats.getOrderId();

			assertThat(actualType).isEqualTo(type);
			assertThat(actualOrderId).isEqualTo(orderId);
		}

		private void assertStateStats(StateStats stateStats, SecurityState from, SecurityState to) {
			SecurityState actualFrom = stateStats.getFrom();
			SecurityState actualTo = stateStats.getTo();

			assertThat(actualFrom).isEqualTo(from);
			assertThat(actualTo).isEqualTo(to);
		}

		private void assertTrade(Trade trade, long sellId, long buyId, int price, int quantity) {
			long actualSellId = trade.getSell().getOrderId();
			long actualBuyId = trade.getBuy().getOrderId();
			int actualPrice = trade.getPrice();
			int actualQuantity = trade.getQuantity();

			assertThat(actualSellId).isEqualTo(sellId);
			assertThat(actualBuyId).isEqualTo(buyId);
			assertThat(actualPrice).isEqualTo(price);
			assertThat(actualQuantity).isEqualTo(quantity);
		}

		private void assertOrderInQueue(
			Side side,
			int idx,
			long orderId,
			int quantity,
			int minExecutionQuantity,
			int price
		) {
			Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
			long actualId = order.getOrderId();
			int actualQuantity = order.getTotalQuantity();
			int actualPrice = order.getPrice();
			int actualMinExecutionQuantity = order.getMinimumExecutionQuantity();

			assertThat(actualId).isEqualTo(orderId);
			assertThat(actualQuantity).isEqualTo(quantity);
			assertThat(actualMinExecutionQuantity).isEqualTo(minExecutionQuantity);
			assertThat(actualPrice).isEqualTo(price);
		}

		private void assertOrderInQueue(Side side, int idx, long orderId, int quantity, int price) {
			assertOrderInQueue(side, idx, orderId, quantity, 0, price);
		}

		private void assertOrderInQueue(
			Side side,
			int idx,
			long orderId,
			int quantity,
			int minExecutionQuantity,
			int price,
			int peakSize,
			int displayedQuantity
		) {
			assertOrderInQueue(side, idx, orderId, quantity, minExecutionQuantity, price);
			Order order = (side == Side.BUY) ? buyQueue.get(idx) : sellQueue.get(idx);
			IcebergOrder iceOrder = (IcebergOrder) order;
			int actualPeakSize = iceOrder.getPeakSize();
			int actualDisplayedQuantity = iceOrder.getDisplayedQuantity();

			assertThat(actualPeakSize).isEqualTo(peakSize);
			assertThat(actualDisplayedQuantity).isEqualTo(displayedQuantity);
		}

		private void assertOrderInQueue(
			Side side,
			int idx,
			long orderId,
			int quantity,
			int price,
			int peakSize,
			int displayedQuantity
		) {
			assertOrderInQueue(side, idx, orderId, quantity, 0, price, peakSize, displayedQuantity);
		}

        private long getOrderDelayByIndex(int index) {
            LocalDateTime startTime = orders.get(index).getEntryTimes().get(0);
            LocalDateTime expireTime = orders.get(index).getExpiryDate();        
            return Duration.between(startTime, expireTime).toSeconds() * 1000;
        }
	}

    private class ScenarioGenerator {
        public void expiring_buy_limit_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.BUY, 1);
            scheduleexpiryDate.run();
        }

        public void expiring_buy_stop_limit_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.BUY, 3);
            scheduleexpiryDate.run();
        }

        public void expiring_buy_iceberg_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.BUY, 5);
            scheduleexpiryDate.run();
        }

        public void expiring_sell_limit_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.SELL, 1);
            scheduleexpiryDate.run();
        }

        public void expiring_sell_stop_limit_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.SELL, 3);
            scheduleexpiryDate.run();
        }

        public void expiring_sell_iceberg_order() {
            ScheduleexpiryDate scheduleexpiryDate = new ScheduleexpiryDate(security, Side.SELL, 5);
            scheduleexpiryDate.run();
        }
    }

    @BeforeEach
	void setup() {
		security = Security.builder().lastTradePrice(550).expiringService(new ExpiringService(mockedTimer)).build();
		sellerBroker = Broker.builder().credit(0).build();
		buyerBroker = Broker.builder().credit(32_500).build();
		sellerShareholder = Shareholder.builder().build();
		buyerShareholder = Shareholder.builder().build();
		sellerShareholder.incPosition(security, 85);
		buyerShareholder.incPosition(security, 0);
		orderBook = security.getOrderBook();
		orders =
			Arrays.asList(
				new Order(1, security, Side.BUY, 10, 100, buyerBroker, buyerShareholder, entryTime, entryTime.plusSeconds(1)),
				new Order(2, security, Side.BUY, 10, 200, buyerBroker, buyerShareholder, entryTime),
				new StopLimitOrder(3, security, Side.BUY, 10, 300, buyerBroker, buyerShareholder, entryTime, entryTime.plusHours(3), 575, 100, OrderStatus.NEW),
				new Order(4, security, Side.BUY, 10, 400, buyerBroker, buyerShareholder, entryTime),
				new IcebergOrder(5, security, Side.BUY, 45, 0, 500, buyerBroker, buyerShareholder, entryTime, entryTime.plusDays(4), 10),
				new Order(1, security, Side.SELL, 10, 600, sellerBroker, sellerShareholder, entryTime, entryTime.plusSeconds(1)),
				new Order(2, security, Side.SELL, 10, 700, sellerBroker, sellerShareholder, entryTime),
				new StopLimitOrder(3, security, Side.SELL, 10, 800, sellerBroker, sellerShareholder, entryTime, entryTime.plusHours(3), 525, 100, OrderStatus.NEW),
				new Order(4, security, Side.SELL, 10, 900, sellerBroker, sellerShareholder, entryTime),
				new IcebergOrder(5, security, Side.SELL, 45, 0, 1000, sellerBroker, sellerShareholder, entryTime, entryTime.plusDays(4), 10)
			);
		orders.forEach(order -> security.addNewOrder(order));
		assertPack = new AssertingPack();
		scenarioGenerator = new ScenarioGenerator();
	}

    @Test
    public void buy_limit_order_expire_scheduling_check() {
        long delay = assertPack.getOrderDelayByIndex(0);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.BUY, 1), delay);
    }
    
    @Test
    public void buy_stop_limit_order_expire_scheduling_check() {    
        long delay = assertPack.getOrderDelayByIndex(2);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.BUY, 3), delay);
    }
    
    @Test
    public void buy_iceberg_order_expire_scheduling_check() {
        long delay = assertPack.getOrderDelayByIndex(4);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.BUY, 5), delay);
    }
    
    @Test
    public void sell_limit_order_expire_scheduling_check() {
        long delay = assertPack.getOrderDelayByIndex(5);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.SELL, 1), delay);
    }
    
    @Test
    public void sell_stop_limit_order_expire_scheduling_check() {
        long delay = assertPack.getOrderDelayByIndex(7);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.SELL, 3), delay);
    }
    
    @Test
    public void sell_iceberg_order_expire_scheduling_check() {
        long delay = assertPack.getOrderDelayByIndex(9);
        Mockito.verify(mockedTimer).schedule(new ScheduleexpiryDate(security, Side.BUY, 5), delay);
    }
    
    @Test
    public void expiring_buy_limit_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.exceptedBuyerCredit = 1000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_buy_limit_order_and_check_buyer_position() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_buy_limit_order_and_check_buy_queue() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.assertOrderInQueue(Side.BUY, 2, 2, 10, 200);
        assertThat(orderBook.getBuyQueue().size()).isEqualTo(3);
    }

    @Test
    public void expiring_buy_limit_order_and_check_seller_credit() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_buy_limit_order_and_check_seller_position() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_buy_limit_order_and_check_sell_queue() {
        scenarioGenerator.expiring_buy_limit_order();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 3, 5, 45, 1000, 10, 10);
        assertThat(orderBook.getSellQueue().size()).isEqualTo(4);
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertPack.exceptedBuyerCredit = 3000;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_buyer_position() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_buy_queue() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertThat(orderBook.getStopLimitOrderBuyQueue()).isEmpty();
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_seller_credit() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_seller_position() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_buy_stop_limit_order_and_check_sell_queue() {
        scenarioGenerator.expiring_buy_stop_limit_order();
        assertPack.assertOrderInStopLimitQueue(Side.SELL, 0, 3, 10, 800, 525);
        assertThat(orderBook.getStopLimitOrderSellQueue().size()).isEqualTo(1);
    }

    @Test
    public void expiring_buy_iceberg_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.exceptedBuyerCredit = 22500;
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_buy_iceberg_order_and_check_buyer_position() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_buy_iceberg_order_and_check_buy_queue() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 4, 10, 400);
        assertThat(orderBook.getBuyQueue().size()).isEqualTo(3);
    }
    
    @Test
    public void expiring_buy_iceberg_order_and_check_seller_credit() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_buy_iceberg_order_and_check_seller_position() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_buy_iceberg_order_and_check_sell_queue() {
        scenarioGenerator.expiring_buy_iceberg_order();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 3, 5, 45, 1000, 10, 10);
        assertThat(orderBook.getSellQueue().size()).isEqualTo(4);
    }

    @Test
    public void expiring_sell_limit_order_and_check_seller_credit() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_sell_limit_order_and_check_seller_position() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_sell_limit_order_and_check_sell_queue() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertOrderInQueue(Side.SELL, 0, 2, 10, 700);
        assertPack.assertOrderInQueue(Side.SELL, 2, 5, 45, 1000, 10, 10);
        assertThat(orderBook.getSellQueue().size()).isEqualTo(3);
    }

    @Test
    public void expiring_sell_limit_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_sell_limit_order_and_check_buyer_position() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_sell_limit_order_and_check_buy_queue() {
        scenarioGenerator.expiring_sell_limit_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 100);
        assertThat(orderBook.getBuyQueue().size()).isEqualTo(4);
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_seller_credit() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_seller_position() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_sell_queue() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertThat(orderBook.getStopLimitOrderSellQueue()).isEmpty();
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_buyer_position() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_sell_stop_limit_order_and_check_buy_queue() {
        scenarioGenerator.expiring_sell_stop_limit_order();
        assertPack.assertOrderInStopLimitQueue(Side.BUY, 0, 3, 10, 300, 575);
        assertThat(orderBook.getStopLimitOrderBuyQueue().size()).isEqualTo(1);
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_seller_credit() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertSellerCredit();
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_seller_position() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertSellerPosition();
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_sell_queue() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertOrderInQueue(Side.SELL, 0, 1, 10, 600);
        assertPack.assertOrderInQueue(Side.SELL, 2, 4, 10, 900);
        assertThat(orderBook.getSellQueue().size()).isEqualTo(3);
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_buyer_credit() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertBuyerCredit();
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_buyer_position() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertBuyerPosition();
    }

    @Test
    public void expiring_sell_iceberg_order_and_check_buy_queue() {
        scenarioGenerator.expiring_sell_iceberg_order();
        assertPack.assertOrderInQueue(Side.BUY, 0, 5, 45, 500, 10, 10);
        assertPack.assertOrderInQueue(Side.BUY, 3, 1, 10, 100);
        assertThat(orderBook.getBuyQueue().size()).isEqualTo(4);
    }
}
