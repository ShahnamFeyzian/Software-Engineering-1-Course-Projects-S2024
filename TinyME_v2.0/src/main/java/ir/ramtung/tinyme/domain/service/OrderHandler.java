package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.BaseOrderRq;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.messaging.request.OrderEntryType;
import ir.ramtung.tinyme.messaging.request.BaseRq;
import ir.ramtung.tinyme.messaging.request.ChangeMatchingStateRq;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderHandler {

	EventPublisher eventPublisher;
	ApplicationServices services;

	public OrderHandler(ApplicationServices services, EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		this.services = services;
	}

	public void handleRq(BaseRq baseRq) {
		try {
			ApplicationServiceResponse response = callService(baseRq);
			publishApplicationServiceResponse(response);
		} catch (InvalidRequestException ex) {
			BaseOrderRq baseOrderRq = (BaseOrderRq) baseRq;
			//FIXME: Add line -1 to fix line +2
			eventPublisher.publish(
					new OrderRejectedEvent(baseOrderRq.getRequestId(), baseOrderRq.getOrderId(), ex.getReasons())
			);
		}
	}

	private ApplicationServiceResponse callService(BaseRq req) {
		if (req instanceof ChangeMatchingStateRq changeMatchingStateRq) {
			return callChangeStateServices(changeMatchingStateRq);
		}
		if (req instanceof DeleteOrderRq deleteReq) {
			return callDeleteServices(deleteReq);
		}

		if (req instanceof EnterOrderRq enterReq) {
			OrderEntryType type = enterReq.getRequestType();
			if (type == OrderEntryType.NEW_ORDER) {
				return callAddServices(enterReq);
			} else if (type == OrderEntryType.UPDATE_ORDER) {
				return callUpdateServices(enterReq);
			}
		}
		throw new InvalidRequestException(Message.UNKNOWN_REQUEST_TYPE);
	}

	private ApplicationServiceResponse callDeleteServices(DeleteOrderRq req) {
		return services.deleteOrder(req);
	}

	private ApplicationServiceResponse callChangeStateServices(ChangeMatchingStateRq req) {
		return services.changeMatchingState(req);
	}

	private ApplicationServiceResponse callAddServices(EnterOrderRq req) {
		if (req.getStopPrice() != 0) {
			return services.addStopLimitOrder(req);
		} else if (req.getPeakSize() != 0) {
			return services.addIcebergOrder(req);
		} else {
			return services.addLimitOrder(req);
		}
	}

	private ApplicationServiceResponse callUpdateServices(EnterOrderRq req) {
		if (req.getStopPrice() != 0) {
			return services.updateStopLimitOrder(req);
		} else if (req.getPeakSize() != 0) {
			return services.updateIcebergOrder(req);
		} else {
			return services.updateLimitOrder(req);
		}
	}

	private void publishApplicationServiceResponse(ApplicationServiceResponse response) {
		List<Event> events = response.getEvents();
		events.forEach(event -> eventPublisher.publish(event));
	}
}
