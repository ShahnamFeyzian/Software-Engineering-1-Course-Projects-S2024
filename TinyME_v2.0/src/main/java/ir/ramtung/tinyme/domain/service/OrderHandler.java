package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.domain.entity.Trade;
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
            publishApplicationServiceResponse(response);
        } 
        catch (InvalidRequestException ex) {
            eventPublisher.publish(new OrderRejectedEvent(enterOrderRq.getRequestId(), enterOrderRq.getOrderId(), ex.getReasons()));
        }
    }

    public void handleDeleteOrder(DeleteOrderRq deleteOrderRq) {
        try {
            ApplicationServiceResponse response = callService(deleteOrderRq);
            publishApplicationServiceResponse(response);
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

    private void publishApplicationServiceResponse(ApplicationServiceResponse response) {
        List<Event> events = createEvents(response);
        events.forEach(event -> eventPublisher.publish(event));
    }

    private List<Event> createEvents(ApplicationServiceResponse response) {
        if (response.isTypeDelete()) {
            return List.of(new OrderDeletedEvent(response.getRequestId(), response.getOrderId()));
        }
        List<Event> events = createFirstMatchResultEvents(response);
        events.addAll(createActivatedEvents(response));
        return events;
    }

    private List<Event> createFirstMatchResultEvents(ApplicationServiceResponse response) {
        if (response.isSuccessful(0)) {
            return createSuccessEvents(response);
        }
        else {
            return createRejectedEvents(response);
        }
    }

    private List<Event> createActivatedEvents(ApplicationServiceResponse response) {
        List<Event> events = new LinkedList<>();
        int numOfMatchResults = response.getMatchResults().size();
        for(int i = 1; i < numOfMatchResults; i++) {
            events.add(new OrderActivatedEvent(response.getOrderId(i)));
            if((response.hasTrades(i))) {
                events.add(createExecutedEvent(response.getOrderId(i), response.getTrades(i)));
            }
        }
        return events;
    }

    private List<Event> createSuccessEvents(ApplicationServiceResponse response) {
        List<Event> events = new LinkedList<>();
        if (response.isTypeAdd()) {
            events.add(new OrderAcceptedEvent(response.getRequestId(), response.getOrderId()));
        }
        else {
            events.add(new OrderUpdatedEvent(response.getRequestId(), response.getOrderId()));
        }
        if (response.hasTrades(0)) {
            events.add(createExecutedEvent(response.getRequestId(), response.getOrderId(), response.getTrades(0)));
        }
        return events;
    }

    private List<Event> createRejectedEvents(ApplicationServiceResponse response) {
        List<Event> events = new LinkedList<>();
        MatchingOutcome outcome = response.getOutcome(0);
        if (outcome == MatchingOutcome.NOT_ENOUGH_CREDIT) 
            events.add(new OrderRejectedEvent(response.getRequestId(), response.getOrderId(), List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT)));
        else if (outcome == MatchingOutcome.NOT_ENOUGH_POSITIONS) 
            events.add(new OrderRejectedEvent(response.getRequestId(), response.getOrderId(), List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS)));
        else if (outcome == MatchingOutcome.NOT_ENOUGH_EXECUTION)
            events.add(new OrderRejectedEvent(response.getRequestId(), response.getOrderId(), List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET)));
        return events;
    }

    private Event createExecutedEvent(long reqId, long orderId, List<Trade> trades) {
        return new OrderExecutedEvent(reqId, orderId, trades.stream().map(TradeDTO::new).collect(Collectors.toList()));
    }

    private Event createExecutedEvent(long orderId, List<Trade> trades) {
        return new OrderExecutedEvent(orderId, trades.stream().map(TradeDTO::new).collect(Collectors.toList()));
    }
}
