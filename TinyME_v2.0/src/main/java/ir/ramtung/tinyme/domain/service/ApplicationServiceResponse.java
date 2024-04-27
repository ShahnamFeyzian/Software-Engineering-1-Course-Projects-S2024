package ir.ramtung.tinyme.domain.service;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import lombok.Getter;

@Getter
public class ApplicationServiceResponse {
    private ApplicationServiceType type;
    private List<MatchResult> matchResults;

    public enum ApplicationServiceType {
        DELETE_ORDER,
        ADD_LIMIT_ORDER,
        UPDATE_LIMIT_ORDER,
        ADD_ICEBERG_ORDER,
        UPDATE_ICEBERG_ORDER,
        ADD_STOP_LIMIT_ORDER,
        UPDATE_STOP_LIMIT_ORDER
    }   

    public ApplicationServiceResponse(ApplicationServiceType type, List<MatchResult> matchResults) {
        this.type = type;
        this.matchResults = matchResults;
    }

    boolean isTypeDelete() {
        return this.type == ApplicationServiceType.DELETE_ORDER;
    }

    boolean isTypeUpdate() {
        return (
            this.type == ApplicationServiceType.UPDATE_LIMIT_ORDER ||
            this.type == ApplicationServiceType.UPDATE_ICEBERG_ORDER ||
            this.type == ApplicationServiceType.UPDATE_STOP_LIMIT_ORDER
        );
    }

    boolean isTypeAdd() {
        return (
            this.type == ApplicationServiceType.ADD_LIMIT_ORDER ||
            this.type == ApplicationServiceType.ADD_ICEBERG_ORDER ||
            this.type == ApplicationServiceType.ADD_STOP_LIMIT_ORDER
        );
    }
}
