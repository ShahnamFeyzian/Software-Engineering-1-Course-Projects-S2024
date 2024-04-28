package ir.ramtung.tinyme.domain.service;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.domain.entity.Trade;
import ir.ramtung.tinyme.messaging.request.BaseOrderRq;
import lombok.Getter;

@Getter
public class ApplicationServiceResponse {
    private ApplicationServiceType type;
    private List<MatchResult> matchResults;
    private BaseOrderRq req;

    public enum ApplicationServiceType {
        DELETE_ORDER,
        ADD_LIMIT_ORDER,
        UPDATE_LIMIT_ORDER,
        ADD_ICEBERG_ORDER,
        UPDATE_ICEBERG_ORDER,
        ADD_STOP_LIMIT_ORDER,
        UPDATE_STOP_LIMIT_ORDER
    }   

    public ApplicationServiceResponse(ApplicationServiceType type, List<MatchResult> matchResults, BaseOrderRq req) {
        this.type = type;
        this.matchResults = matchResults;
        this.req = req;
    }

    public boolean isTypeDelete() {
        return this.type == ApplicationServiceType.DELETE_ORDER;
    }

    public boolean isTypeUpdate() {
        return (
            this.type == ApplicationServiceType.UPDATE_LIMIT_ORDER ||
            this.type == ApplicationServiceType.UPDATE_ICEBERG_ORDER ||
            this.type == ApplicationServiceType.UPDATE_STOP_LIMIT_ORDER
        );
    }

    public boolean isTypeAdd() {
        return (
            this.type == ApplicationServiceType.ADD_LIMIT_ORDER ||
            this.type == ApplicationServiceType.ADD_ICEBERG_ORDER ||
            this.type == ApplicationServiceType.ADD_STOP_LIMIT_ORDER
        );
    }

    public long getRequestId() {
        return req.getRequestId();
    }

    public long getOrderId() {
        return req.getOrderId();
    }

    public long getOrderId(int idx) {
        return matchResults.get(idx).remainder().getOrderId();
    }

    public boolean isSuccessful(int idx) {
        return matchResults.get(idx).isSuccessful();
    }

    public boolean hasTrades(int idx) {
        return !matchResults.get(idx).trades().isEmpty();
    }

    public List<Trade> getTrades(int idx) {
        return matchResults.get(idx).trades();
    }

    public MatchingOutcome getOutcome(int idx) {
        return matchResults.get(idx).outcome();
    }
}
