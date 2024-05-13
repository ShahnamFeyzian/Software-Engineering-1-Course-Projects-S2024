package ir.ramtung.tinyme.domain.entity.security_stats;

import lombok.Getter;

@Getter
public class SituationalStats extends SecurityStats {

    private long orderId;
    private SituationalStatsType type;

    private SituationalStats (long orderId, SituationalStatsType type) {
        this.orderId = orderId;
        this.type = type;
    }

	public static SecurityStats createAddOrderStats(int OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.ADD_ORDER);
	}

	public static SituationalStats createDeleteOrderStats(int OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.DELETE_ORDER);
	}

	public static SituationalStats createUpdateOrderStats(int OrderId) {
		return new SituationalStats(OrderId, SituationalStatsType.UPDATE_ORDER);
	}

    public static SituationalStats createNotEnoughCreditStats(int OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_CREDIT);
    }

    public static SituationalStats createNotEnoughPositionsStats(int OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_POSITIONS);
    }

    public static SituationalStats createNotEnoughExecutionStats(int OrderId) {
        return new SituationalStats(OrderId, SituationalStatsType.NOT_ENOUGH_EXECUTION);
    }
}
