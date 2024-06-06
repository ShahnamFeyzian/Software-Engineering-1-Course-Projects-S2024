package ir.ramtung.tinyme.utils;


import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static ir.ramtung.tinyme.domain.entity.Side.BUY;
import static ir.ramtung.tinyme.utils.TestDefaults.*;

@Component
public class TestOrderBuilder {
    private static int ORDER_ID = 1;

    private final BrokerRepository brokerRepository;
    private final ShareholderRepository shareholderRepository;
    private final SecurityRepository securityRepository;

    private Order.OrderBuilder orderBuilder;

    public TestOrderBuilder(SecurityRepository securityRepository, BrokerRepository brokerRepository, ShareholderRepository shareholderRepository) {
        this.securityRepository = securityRepository;
        this.brokerRepository = brokerRepository;
        this.shareholderRepository = shareholderRepository;
    }

    public TestOrderBuilder createOrder() {
        Broker broker = brokerRepository.findBrokerById(BROKER_ID);
        if (broker == null)
            throw new IllegalStateException("Default Broker Not Defined");

        Shareholder shareholder = shareholderRepository.findShareholderById(SHAREHOLDER_ID);
        if (shareholder == null)
            throw new IllegalStateException("Default Shareholder Not Defined");

        Security security = securityRepository.findSecurityByIsin(ISIN);
        if (security == null)
            throw new IllegalStateException("Default Security Not Defined");

        orderBuilder = Order.builder()
                .orderId(ORDER_ID++)
                .security(security)
                .broker(broker)
                .shareholder(shareholder)
                .side(BUY)
                .initialQuantity(QUANTITY)
                .quantity(QUANTITY)
                .price(PRICE)
                .status(OrderStatus.NEW)
                .entryTime(LocalDateTime.now())
                .minimumExecutionQuantity(0);
        return this;
    }

    public TestOrderBuilder createOrder(Security security, Shareholder shareholder, Broker broker) {
        orderBuilder = Order.builder()
                .orderId(ORDER_ID++)
                .security(security)
                .broker(broker)
                .shareholder(shareholder)
                .side(BUY)
                .initialQuantity(QUANTITY)
                .quantity(QUANTITY)
                .price(PRICE)
                .status(OrderStatus.NEW)
                .entryTime(LocalDateTime.now())
                .minimumExecutionQuantity(0);
        return this;
    }

    public TestOrderBuilder createIcebergOrder() {
        Security security = securityRepository.findSecurityByIsin(ISIN);
        if (security == null)
            throw new IllegalStateException("Default security Not Defined");

        Broker broker = brokerRepository.findBrokerById(BROKER_ID);
        if (broker == null)
            throw new IllegalStateException("Default Broker Not Defined");

        Shareholder shareholder = shareholderRepository.findShareholderById(SHAREHOLDER_ID);
        if (shareholder == null)
            throw new IllegalStateException("Default Shareholder Not Defined");

        orderBuilder = IcebergOrder.builder()
                .orderId(ORDER_ID++)
                .security(security)
                .broker(broker)
                .shareholder(shareholder)
                .side(BUY)
                .initialQuantity(QUANTITY)
                .quantity(QUANTITY)
                .peakSize(PEAK_SIZE)
                .displayedQuantity(PEAK_SIZE)
                .price(PRICE)
                .status(OrderStatus.NEW)
                .entryTime(LocalDateTime.now())
                .minimumExecutionQuantity(0);
        return this;
    }

    public TestOrderBuilder createIcebergOrder(Security security, Shareholder shareholder, Broker broker) {
        orderBuilder = IcebergOrder.builder()
                .orderId(ORDER_ID++)
                .security(security)
                .broker(broker)
                .shareholder(shareholder)
                .side(BUY)
                .initialQuantity(QUANTITY)
                .quantity(QUANTITY)
                .peakSize(PEAK_SIZE)
                .displayedQuantity(PEAK_SIZE)
                .price(PRICE)
                .status(OrderStatus.NEW)
                .entryTime(LocalDateTime.now())
                .minimumExecutionQuantity(0);
        return this;
    }

    public Order build() {
        Order order = orderBuilder.build();
        orderBuilder = null;
        return order;
    }

    public TestOrderBuilder price(int price) {
        orderBuilder.price(price);
        return this;
    }

    public TestOrderBuilder quantity(int quantity) {
        orderBuilder
                .initialQuantity(quantity)
                .quantity(quantity);
        return this;
    }

    public TestOrderBuilder quantity(int quantity, int peakSize) {
        if (!(orderBuilder instanceof IcebergOrder.IcebergOrderBuilder<?, ?>))
            throw new IllegalStateException("Can not set peak size for order");

        ((IcebergOrder.IcebergOrderBuilder<?, ?>) orderBuilder)
                .peakSize(peakSize)
                .displayedQuantity(Math.min(quantity, peakSize))
                .initialQuantity(quantity)
                .quantity(quantity);
        return this;
    }

    public TestOrderBuilder side(Side side) {
        orderBuilder.side(side);
        return this;
    }

    public TestOrderBuilder minimumExecutionQuantity(int minimumExecutionQuantity) {
        orderBuilder.minimumExecutionQuantity(minimumExecutionQuantity);
        return this;
    }

}
