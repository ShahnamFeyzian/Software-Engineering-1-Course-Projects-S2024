package ir.ramtung.tinyme.domain.entity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import lombok.Getter;

@Getter
public class SecurityStats {

	private int OrderId;
	private SecurityStatsType type;
	private ArrayList<Trade> trades;

	private SecurityStats(int OrderId, SecurityStatsType type, ArrayList<Trade> trades) {
		this.OrderId = OrderId;
		this.type = type;
		this.trades = trades;
	}

	public static SecurityStats createAddOrderStats(int OrderId) {
		return new SecurityStats(OrderId, SecurityStatsType.ADD_ORDER, null);
	}

	public static SecurityStats createDeleteOrderStats(int OrderId) {
		return new SecurityStats(OrderId, SecurityStatsType.DELETE_ORDER, null);
	}

	public static SecurityStats createUpdateOrderStats(int OrderId) {
		return new SecurityStats(OrderId, SecurityStatsType.UPDATE_ORDER, null);
	}

	public static SecurityStats createExecuteOrderStats(int OrderId, ArrayList<Trade> trades) {
		return new SecurityStats(OrderId, SecurityStatsType.EXECUTE_ORDER, trades);
	}

    public static SecurityStats createNotEnoughCreditStats(int OrderId) {
        return new SecurityStats(OrderId, SecurityStatsType.NOT_ENOUGH_CREDIT, null);
    }

    public static SecurityStats createNotEnoughPositionsStats(int OrderId) {
        return new SecurityStats(OrderId, SecurityStatsType.NOT_ENOUGH_POSITIONS, null);
    }

    public static SecurityStats createNotEnoughExecutionStats(int OrderId) {
        return new SecurityStats(OrderId, SecurityStatsType.NOT_ENOUGH_EXECUTION, null);
    }

	public ArrayList<Trade> getTrades() {
		if (this.type == SecurityStatsType.EXECUTE_ORDER) {
			return this.trades;
		}
		throw new IllegalStateException("This is not an execute order stats");
	}
}
