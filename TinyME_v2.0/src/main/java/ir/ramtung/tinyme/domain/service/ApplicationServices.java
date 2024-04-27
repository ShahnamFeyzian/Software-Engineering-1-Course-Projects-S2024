package ir.ramtung.tinyme.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.InvalidIcebergPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;

@Service
public class ApplicationServices {
    private SecurityRepository securityRepository;
    private BrokerRepository brokerRepository;
    private ShareholderRepository shareholderRepository;
    private Matcher matcher;

    public ApplicationServiceResponse deleteOrder(DeleteOrderRq req) {
        validateDeleteOrderRq(req);
        return new ApplicationServiceResponse(null, null);
    }

    public ApplicationServiceResponse addLimitOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateLimitOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        validateUpdateOrderRq(req);
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse addIcebergOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateIcebergOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        validateUpdateOrderRq(req);
        return new ApplicationServiceResponse(null, null);
    }

    public ApplicationServiceResponse addStopLimitOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        return new ApplicationServiceResponse(null, null);
    }
    
    public ApplicationServiceResponse updateStopLimitOrder(EnterOrderRq req) {
        generalEnterOrderValidation(req);
        validateUpdateOrderRq(req);
        return new ApplicationServiceResponse(null, null);
    }

    private void validateDeleteOrderRq(DeleteOrderRq deleteOrderRq) {
        List<String> errors = deleteOrderRq.validateYourFields();
        if (!securityRepository.isThereSecurityWithIsin(deleteOrderRq.getSecurityIsin()))
            errors.add(Message.UNKNOWN_SECURITY_ISIN);
        if (!errors.isEmpty())
            throw new InvalidRequestException(errors);
        Security security = securityRepository.findSecurityByIsin(deleteOrderRq.getSecurityIsin());
        if (!security.isThereOrderWithId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId()))
            throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
    }

    private void generalEnterOrderValidation(EnterOrderRq enterOrderRq) {
        List<String> errors = enterOrderRq.validateYourFields();
        if (!securityRepository.isThereSecurityWithIsin(enterOrderRq.getSecurityIsin()))
            errors.add(Message.UNKNOWN_SECURITY_ISIN);
        else {
            Security security = securityRepository.findSecurityByIsin(enterOrderRq.getSecurityIsin());
            errors.addAll(security.checkLotAndTickSize(enterOrderRq));
        }
        if (!brokerRepository.isThereBrokerWithId(enterOrderRq.getBrokerId()))
            errors.add(Message.UNKNOWN_BROKER_ID);
        if (!shareholderRepository.isThereShareholderWithId(enterOrderRq.getShareholderId())) 
            errors.add(Message.UNKNOWN_SHAREHOLDER_ID);
        if (!errors.isEmpty())
            throw new InvalidRequestException(errors);
    }
    
    private void validateUpdateOrderRq (EnterOrderRq updateOrderRq) {
        try {
            Security security = securityRepository.findSecurityByIsin(updateOrderRq.getSecurityIsin());
            Order order = security.findByOrderId(updateOrderRq.getSide(), updateOrderRq.getOrderId());
            order.checkNewPeakSize(updateOrderRq.getPeakSize());
            order.checkNewMinimumExecutionQuantity(updateOrderRq.getMinimumExecutionQuantity());
            order.checkNewStopLimitPrice(updateOrderRq.getStopPrice());
        }
        catch (NotFoundException exp) {
            throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
        }
        catch (InvalidIcebergPeakSizeException exp) {
            throw new InvalidRequestException(Message.CANNOT_SPECIFY_0_PEAK_SIZE_FOR_A_ICEBERG_ORDER);
        }
        catch (InvalidPeakSizeException exp) {
            throw new InvalidRequestException(Message.INVALID_PEAK_SIZE);
        }
        catch (UpdateMinimumExecutionQuantityException exp) {
            throw new InvalidRequestException(Message.CANNOT_UPDATE_MINIMUM_EXECUTION_QUANTITY);
        }
        catch (InvalidStopLimitPriceException exp){
            throw new InvalidRequestException(Message.INVALID_STOP_LIMIT_UPDATE_PRICE);
        }
    }
}
