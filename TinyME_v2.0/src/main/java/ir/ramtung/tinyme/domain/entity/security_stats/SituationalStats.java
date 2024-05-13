package ir.ramtung.tinyme.domain.entity.security_stats;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import lombok.Getter;

@Getter
public class SituationalStats extends SecurityStats {

    private long orderId;
    private SituationalStatsType type;

    private SituationalStats (long orderId, SituationalStatsType type) {
        this.orderId = orderId;
        this.type = type;
    }

	public static SecurityStats createAddOrderStats(long OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.ADD_ORDER);
	}

	public static SituationalStats createDeleteOrderStats(long OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.DELETE_ORDER);
	}

	public static SituationalStats createUpdateOrderStats(long OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.UPDATE_ORDER);
	}

    public static SituationalStats createNotEnoughCreditStats(long OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_CREDIT);
    }

    public static SituationalStats createNotEnoughPositionsStats(long OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_POSITIONS);
    }

    public static SituationalStats createNotEnoughExecutionStats(long OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_EXECUTION);
    }

    public static SituationalStats createOrderActivatedStats(long OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.ORDER_ACTIVATED);
    }

    public static SituationalStats createExecutionStatsFromUnsuccessfulMatchResult(MatchResult matchResult, long orderId) {
        switch (matchResult.outcome()) {
            case MatchingOutcome.NOT_ENOUGH_CREDIT   : return createNotEnoughCreditStats(orderId);
            case MatchingOutcome.NOT_ENOUGH_POSITIONS: return createNotEnoughPositionsStats(orderId);
            case MatchingOutcome.NOT_ENOUGH_EXECUTION: return createNotEnoughExecutionStats(orderId);
            default: throw new IllegalArgumentException("Unknown unsuccessful match result");
        }
    }
}
