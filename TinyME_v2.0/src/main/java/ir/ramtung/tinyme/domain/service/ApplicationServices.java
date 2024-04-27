package ir.ramtung.tinyme.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;

@Service
public class ApplicationServices {
    
    public ApplicationServiceResponse deleteOrder(DeleteOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }

    public ApplicationServiceResponse addLimitOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateLimitOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse addIcebergOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateIcebergOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }

    public ApplicationServiceResponse addStopLimitOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateStopLimitOrder(EnterOrderRq req) {
        return new ApplicationServiceResponse(null, null);
    }
}
