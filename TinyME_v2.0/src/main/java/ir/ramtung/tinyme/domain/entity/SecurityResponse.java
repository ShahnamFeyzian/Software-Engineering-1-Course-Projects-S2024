package ir.ramtung.tinyme.domain.entity;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import lombok.Getter;

@Getter
public final class SecurityResponse {
	private final List<SecurityStats> stats;

	public SecurityResponse(List<SecurityStats> stats) {
		this.stats = stats;
	}
}
