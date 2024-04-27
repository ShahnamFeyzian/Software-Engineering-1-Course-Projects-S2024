package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.InvalidIcebergPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.request.BaseOrderRq;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.messaging.request.OrderEntryType;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderHandler {
    SecurityRepository securityRepository;
    BrokerRepository brokerRepository;
    ShareholderRepository shareholderRepository;
    EventPublisher eventPublisher;
    Matcher matcher;
    ApplicationServices services;

    public OrderHandler(ApplicationServices services, SecurityRepository securityRepository, BrokerRepository brokerRepository, ShareholderRepository shareholderRepository, EventPublisher eventPublisher, Matcher matcher) {
        this.securityRepository = securityRepository;
        this.brokerRepository = brokerRepository;
        this.shareholderRepository = shareholderRepository;
        this.eventPublisher = eventPublisher;
        this.matcher = matcher;
        this.services = services;
    }

    public void handleEnterOrder(EnterOrderRq enterOrderRq) {
        try {
            ApplicationServiceResponse response = callService(enterOrderRq);
            publishApplicationServiceResponse(response, enterOrderRq);
        } 
        catch (InvalidRequestException ex) {
            eventPublisher.publish(new OrderRejectedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), ex.getReasons()));
        }
    }

    public void handleDeleteOrder(DeleteOrderRq deleteOrderRq) {
        try {
            ApplicationServiceResponse response = callService(deleteOrderRq);
            publishApplicationServiceResponse(response, deleteOrderRq);
        } 
        catch (InvalidRequestException ex) {
            eventPublisher.publish(new OrderRejectedEvent(deleteOrderRq.getRequestId(), deleteOrderRq.getOrderId(), ex.getReasons()));
        }
    }

    private ApplicationServiceResponse callService(BaseOrderRq req) {
        if(req instanceof DeleteOrderRq deleteReq) {
            return callDeleteServices(deleteReq);
        }
        
        if(req instanceof EnterOrderRq enterReq) {
            OrderEntryType type = enterReq.getRequestType();
            if (type == OrderEntryType.NEW_ORDER) {
                return callAddServices(enterReq);
            }
            else if (type == OrderEntryType.UPDATE_ORDER) {
                return callUpdateServices(enterReq);
            }
        }
        throw new InvalidRequestException(Message.UNKNOWN_REQUEST_TYPE);
    }

    private ApplicationServiceResponse callDeleteServices(DeleteOrderRq req) {
        return services.deleteOrder(req);
    }

    private ApplicationServiceResponse callAddServices(EnterOrderRq req) {
        if (req.getStopPrice() != 0) {
            return services.addStopLimitOrder(req);
        }
        else if (req.getPeakSize() != 0) {
            return services.addIcebergOrder(req);
        }
        else {
            return services.addLimitOrder(req);
        }
    }

    private ApplicationServiceResponse callUpdateServices(EnterOrderRq req) {
        if (req.getStopPrice() != 0) {
            return services.updateStopLimitOrder(req);
        }
        else if (req.getPeakSize() != 0) {
            return services.updateIcebergOrder(req);
        }
        else {
            return services.updateLimitOrder(req);
        }
    }

    private List<MatchResult> runEnterOrderRq(EnterOrderRq enterOrderRq) {
        Order tempOrder = createTempOrderByEnterOrderRq(enterOrderRq);
        if (enterOrderRq.getRequestType() == OrderEntryType.NEW_ORDER)
            return tempOrder.getSecurity().addNewOrder(tempOrder, matcher);
        else if (enterOrderRq.getStopPrice() != 0)
            return tempOrder.getSecurity().updateSloOrder((StopLimitOrder) tempOrder, matcher);
        else
            return tempOrder.getSecurity().updateOrder(tempOrder, matcher);
    }

    private Order createTempOrderByEnterOrderRq(EnterOrderRq enterOrderRq) {
        Broker broker = brokerRepository.findBrokerById(enterOrderRq.getBrokerId());
        Shareholder shareholder = shareholderRepository.findShareholderById(enterOrderRq.getShareholderId());
        Security security = securityRepository.findSecurityByIsin(enterOrderRq.getSecurityIsin());
        
        if (enterOrderRq.getStopPrice() != 0) 
            return new StopLimitOrder(
                enterOrderRq.getOrderId(), security, enterOrderRq.getSide(), 
                enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, 
                shareholder, enterOrderRq.getStopPrice()
            );
        else if (enterOrderRq.getPeakSize() != 0)
            return new IcebergOrder(
                enterOrderRq.getOrderId(), security, enterOrderRq.getSide(),
                enterOrderRq.getQuantity(), enterOrderRq.getMinimumExecutionQuantity(), 
                enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime(), enterOrderRq.getPeakSize()
            );
        else
            return new Order(
                enterOrderRq.getOrderId(), security, enterOrderRq.getSide(),
                enterOrderRq.getQuantity(), enterOrderRq.getMinimumExecutionQuantity(), 
                enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime()
            );
    }

    private void publishApplicationServiceResponse(ApplicationServiceResponse response, BaseOrderRq enterOrderRq) {
        
    }

    private List<Event> createEvents(List<MatchResult> matchResults, EnterOrderRq enterOrderRq) {
        MatchResult matchResult = matchResults.getFirst();
        List<Event> events = new LinkedList<>();
        if (matchResult.isSuccessful())
            events.addAll(createSuccessEvents(matchResult, enterOrderRq));
        else
            events.addAll(createRejectedEvents(matchResult, enterOrderRq));

        for(int i = 1; i < matchResults.size(); i++) {
            events.add(new OrderActivatedEvent(matchResults.get(i).remainder().getOrderId()));
            if((!matchResults.get(i).trades().isEmpty()))
                events.add(new OrderExecutedEvent(matchResults.get(i).remainder().getOrderId(), matchResults.get(i).trades().stream().map(TradeDTO::new).collect(Collectors.toList())));
        }

        return events;
    }

    private List<Event> createRejectedEvents(MatchResult matchResult, EnterOrderRq enterOrderRq) {
        List<Event> events = new LinkedList<>();
        if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_CREDIT) 
            events.add(new OrderRejectedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT)));
        else if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_POSITIONS) 
            events.add(new OrderRejectedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS)));
        else if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_EXECUTION)
            events.add(new OrderRejectedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET)));
        return events;
    }

    private List<Event> createSuccessEvents(MatchResult matchResult, EnterOrderRq enterOrderRq) {
        List<Event> events = new LinkedList<>();
        if (enterOrderRq.getRequestType() == OrderEntryType.NEW_ORDER)
            events.add(new OrderAcceptedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId()));
        else
            events.add(new OrderUpdatedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId()));
        if (!matchResult.trades().isEmpty()) 
            events.add(new OrderExecutedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), matchResult.trades().stream().map(TradeDTO::new).collect(Collectors.toList())));
        return events;
    }

    private void validateEnterOrderRq(EnterOrderRq enterOrderRq) {
        generalEnterOrderValidation(enterOrderRq);
        if (enterOrderRq.getRequestType() == OrderEntryType.UPDATE_ORDER)
            validateUpdateOrderRq(enterOrderRq, securityRepository.findSecurityByIsin(enterOrderRq.getSecurityIsin()));
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

    private void validateUpdateOrderRq(EnterOrderRq updateOrderRq, Security security) {
        try {
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
}
