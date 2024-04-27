package ir.ramtung.tinyme.domain.service;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;

public class ApplicationServices {
    
    public List<MatchResult> deleteOrder(DeleteOrderRq req) {
        return List.of();
    }

    public List<MatchResult> addLimitOrder(EnterOrderRq req) {
        return List.of();
    }
    
    public List<MatchResult> updateLimitOrder(EnterOrderRq req) {
        return List.of();
    }
    
    public List<MatchResult> addIcebergOrder(EnterOrderRq req) {
        return List.of();
    }
    
    public List<MatchResult> updateIcebergOrder(EnterOrderRq req) {
        return List.of();
    }

    public List<MatchResult> addStopLimitOrder(EnterOrderRq req) {
        return List.of();
    }
    
    public List<MatchResult> updateStopLimitOrder(EnterOrderRq req) {
        return List.of();
    }
}
