package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.MatchResult;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.utils.FixtureDefaults;
import ir.ramtung.tinyme.utils.TestOrderBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static ir.ramtung.tinyme.domain.service.MatchingOutcome.OK;
import static ir.ramtung.tinyme.domain.service.MatchingOutcome.MINIMUM_QUANTITY_NOT_SATISFIED;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
//this class will probably merge with MatcherTest but for introducing new style for utils it is seperated.
public class MatchingTest {

    @Autowired
    private Matcher matcher;

    @Autowired
    private TestOrderBuilder orderBuilder;

    @Autowired
    private FixtureDefaults fixture;

    private Security security;

    @BeforeEach
    public void setUp() {
        fixture.init();
        security = fixture.getSecurity();
        security.getOrderBook().enqueue(
                orderBuilder.createOrder()
                        .price(101)
                        .side(Side.BUY)
                        .quantity(70)
                        .build()
        );
    }

    @AfterEach
    public void teardown() {
        fixture.flush();
    }

    @Test
    void order_successfully_match_when_MEQ_is_satisfied() {
        Order newOrder = orderBuilder.createOrder()
                .side(Side.SELL)
                .quantity(100)
                .minimumExecutionQuantity(50)
                .build();

        MatchResult result = matcher.execute(newOrder);

        assertThat(result.outcome()).isEqualTo(OK);
        assertThat(result.trades()).hasSize(1);
        assertThat(result.remainder().getQuantity()).isEqualTo(30);
    }

    @Test
    void order_not_matched_when_MEQ_is_not_satisfied() {
        Order newOrder = orderBuilder.createOrder()
                .side(Side.SELL)
                .quantity(200)
                .minimumExecutionQuantity(100)
                .build();

        MatchResult result = matcher.execute(newOrder);

        assertThat(result.outcome()).isEqualTo(MINIMUM_QUANTITY_NOT_SATISFIED);
        assertThat(result.trades()).hasSize(0);
    }

    @Test
    void MEQ_works_with_iceberg_order() {
        IcebergOrder newOrder = (IcebergOrder) orderBuilder.createIcebergOrder()
                .side(Side.SELL)
                .quantity(100, 20)
                .minimumExecutionQuantity(50)
                .build();

        MatchResult result = matcher.execute(newOrder);

        assertThat(result.outcome()).isEqualTo(OK);
        assertThat(result.trades()).hasSize(1);
        assertThat(result.remainder().getQuantity()).isEqualTo(20);
    }


}
