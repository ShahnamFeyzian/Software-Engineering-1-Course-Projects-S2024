package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.request.BaseOrderRq;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.messaging.request.OrderEntryType;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderHandler {
    EventPublisher eventPublisher;
    ApplicationServices services;

    public OrderHandler(ApplicationServices services, EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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

    private void publishApplicationServiceResponse(ApplicationServiceResponse response, BaseOrderRq orderRq) {
        if (response.isTypeDelete()) {
            eventPublisher.publish(new OrderDeletedEvent(orderRq.getRequestId(), orderRq.getOrderId()));
            return;
        }
        List<Event> events = createEvents(response, orderRq);
        events.forEach(event -> eventPublisher.publish(event));
    }

    private List<Event> createEvents(ApplicationServiceResponse response, BaseOrderRq orderRq) {
        MatchResult matchResult = response.getMatchResults().getFirst();
        List<Event> events = new LinkedList<>();
        if (matchResult.isSuccessful())
            events.addAll(createSuccessEvents(response, orderRq));
        else
            events.addAll(createRejectedEvents(response, orderRq));

        List<MatchResult> matchResults = response.getMatchResults();
        for(int i = 1; i < matchResults.size(); i++) {
            events.add(new OrderActivatedEvent(matchResults.get(i).remainder().getOrderId()));
            if((!matchResults.get(i).trades().isEmpty()))
                events.add(new OrderExecutedEvent(matchResults.get(i).remainder().getOrderId(), matchResults.get(i).trades().stream().map(TradeDTO::new).collect(Collectors.toList())));
        }

        return events;
    }

    private List<Event> createRejectedEvents(ApplicationServiceResponse response, BaseOrderRq orderRq) {
        List<Event> events = new LinkedList<>();
        MatchResult matchResult = response.getMatchResults().getFirst();
        if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_CREDIT) 
            events.add(new OrderRejectedEvent(orderRq.getRequestId(), orderRq.getOrderId(), List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT)));
        else if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_POSITIONS) 
            events.add(new OrderRejectedEvent(orderRq.getRequestId(), orderRq.getOrderId(), List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS)));
        else if (matchResult.outcome() == MatchingOutcome.NOT_ENOUGH_EXECUTION)
            events.add(new OrderRejectedEvent(orderRq.getRequestId(), orderRq.getOrderId(), List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET)));
        return events;
    }

    private List<Event> createSuccessEvents(ApplicationServiceResponse response, BaseOrderRq orderRq) {
        List<Event> events = new LinkedList<>();
        if (response.isTypeAdd())
            events.add(new OrderAcceptedEvent(orderRq.getRequestId(), orderRq.getOrderId()));
        else
            events.add(new OrderUpdatedEvent(orderRq.getRequestId(), orderRq.getOrderId()));
        
        MatchResult matchResult = response.getMatchResults().getFirst(); 
        if (!matchResult.trades().isEmpty()) 
            events.add(new OrderExecutedEvent(orderRq.getRequestId(), orderRq.getOrderId(), matchResult.trades().stream().map(TradeDTO::new).collect(Collectors.toList())));
        return events;
    }
}
