package ir.ramtung.tinyme.domain.entity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public final class SecurityResponse {

	// private final MatchingOutcome outcome;
	// private final Order remainder;
	// private final LinkedList<Trade> trades;
	private final ArrayList<SecurityStats> stats;

	public SecurityResponse(ArrayList<SecurityStats> stats) {
		this.stats = stats;
	}

//	 public static SecurityResponse executed(Order remainder, List<Trade> trades) {
//	 	return new SecurityResponse(MatchingOutcome.EXECUTED, remainder, new LinkedList<>(trades));
//	 }

	// public static SecurityResponse notEnoughCredit() {
	// 	return new SecurityResponse(MatchingOutcome.NOT_ENOUGH_CREDIT, null, new LinkedList<>());
	// }

	// public static SecurityResponse notEnoughPositions() {
	// 	return new SecurityResponse(MatchingOutcome.NOT_ENOUGH_POSITIONS, null, new LinkedList<>());
	// }

	// public static SecurityResponse notEnoughExecution() {
	// 	return new SecurityResponse(MatchingOutcome.NOT_ENOUGH_EXECUTION, null, new LinkedList<>());
	// }

	// private SecurityResponse(MatchingOutcome outcome, Order remainder, LinkedList<Trade> trades) {
	// 	this.outcome = outcome;
	// 	this.remainder = remainder;
	// 	this.trades = trades;
	// }

	// public MatchingOutcome outcome() {
	// 	return outcome;
	// }

	// public Order remainder() {
	// 	return remainder;
	// }

	// public LinkedList<Trade> trades() {
	// 	return trades;
	// }

	// public boolean isSuccessful() {
	// 	return outcome == MatchingOutcome.EXECUTED;
	// }

	// @Override
	// public boolean equals(Object obj) {
	// 	if (obj == this) {
	// 		return true;
	// 	}

	// 	if (obj == null || obj.getClass() != this.getClass()) {
	// 		return false;
	// 	}

	// 	var that = (SecurityResponse) obj;
	// 	return Objects.equals(this.remainder, that.remainder) && Objects.equals(this.trades, that.trades);
	// }

	// @Override
	// public int hashCode() {
	// 	return Objects.hash(remainder, trades);
	// }

	// @Override
	// public String toString() {
	// 	return "MatchResult[" + "remainder=" + remainder + ", " + "trades=" + trades + ']';
	// }
}
