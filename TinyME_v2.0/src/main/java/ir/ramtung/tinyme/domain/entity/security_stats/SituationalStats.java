package ir.ramtung.tinyme.domain.entity.security_stats;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import lombok.Getter;

@Getter
public class SituationalStats extends SecurityStats {

    private long orderId;
    private long requestId;
    private SituationalStatsType type;

    private SituationalStats (long orderId, SituationalStatsType type) {
        this.orderId = orderId;
        this.type = type;
    }

    private SituationalStats (long orderId, long requestId, SituationalStatsType type) {
        this(orderId, type);
        this.requestId = requestId;
    }

	public static SecurityStats createAddOrderStats(long orderId) {
		return new SituationalStats(orderId, SituationalStatsType.ADD_ORDER);
	}

	public static SituationalStats createDeleteOrderStats(long orderId) {
		return new SituationalStats(orderId, SituationalStatsType.DELETE_ORDER);
	}

	public static SituationalStats createUpdateOrderStats(long orderId) {
		return new SituationalStats(orderId, SituationalStatsType.UPDATE_ORDER);
	}

    public static SituationalStats createNotEnoughCreditStats(long orderId) {
        return new SituationalStats(orderId, SituationalStatsType.NOT_ENOUGH_CREDIT);
    }

    public static SituationalStats createNotEnoughPositionsStats(long orderId) {
        return new SituationalStats(orderId, SituationalStatsType.NOT_ENOUGH_POSITIONS);
    }

    public static SituationalStats createNotEnoughExecutionStats(long orderId) {
        return new SituationalStats(orderId, SituationalStatsType.NOT_ENOUGH_EXECUTION);
    }

    public static SituationalStats createOrderActivatedStats(long orderId, long requestId) {
        return new SituationalStats(orderId, requestId, SituationalStatsType.ORDER_ACTIVATED);
    }

    public static SituationalStats createExecutionStatsFromUnsuccessfulMatchResult(MatchResult matchResult, long orderId) {
        switch (matchResult.outcome()) {
            case MatchingOutcome.NOT_ENOUGH_CREDIT   : return createNotEnoughCreditStats(orderId);
            case MatchingOutcome.NOT_ENOUGH_POSITIONS: return createNotEnoughPositionsStats(orderId);
            case MatchingOutcome.NOT_ENOUGH_EXECUTION: return createNotEnoughExecutionStats(orderId);
            default: throw new IllegalArgumentException("Unknown unsuccessful match result");
        }
    }

    public long getRequestId() {
        if (this.type != SituationalStatsType.ORDER_ACTIVATED) {
            throw new IllegalStateException("Only order activated stats has requestId.");
        }
        return this.requestId;
    }
}
