package ir.ramtung.tinyme.domain.entity;

import java.util.ArrayList;
import java.util.List;

import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import lombok.Getter;

@Getter
public final class SecurityResponse {
	private List<SecurityStats> stats = new ArrayList<>();

	public SecurityResponse(List<SecurityStats> stats) {
		this.stats = stats;
	}

	public SecurityResponse(SecurityStats stats) {
		this.stats.add(stats);
	}
}
