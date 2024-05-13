package ir.ramtung.tinyme.domain.entity;

import java.util.ArrayList;

import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import lombok.Getter;

@Getter
public final class SecurityResponse {
	private final ArrayList<SecurityStats> stats;

	public SecurityResponse(ArrayList<SecurityStats> stats) {
		this.stats = stats;
	}
}
