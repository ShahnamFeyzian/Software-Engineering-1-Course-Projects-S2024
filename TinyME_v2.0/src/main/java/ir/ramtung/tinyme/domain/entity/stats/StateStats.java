package ir.ramtung.tinyme.domain.entity.stats;

import ir.ramtung.tinyme.domain.entity.SecurityState;
import lombok.Getter;

@Getter
public class StateStats extends SecurityStats {

	private SecurityState from;
	private SecurityState to;

	private StateStats(SecurityState from, SecurityState to) {
		this.from = from;
		this.to = to;
	}

	public static StateStats createStateStats(SecurityState from, SecurityState to) {
		return new StateStats(from, to);
	}
}
