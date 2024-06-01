package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.exception.InvalidRequestFieldAccess;
import ir.ramtung.tinyme.messaging.event.Event;
import ir.ramtung.tinyme.messaging.request.BaseOrderRq;
import java.util.List;

import ir.ramtung.tinyme.messaging.request.BaseRq;
import lombok.Getter;

@Getter
public class ApplicationServiceResponse {

	private ApplicationServiceType type;
	private List<Event> events;
	private BaseRq req;

	public enum ApplicationServiceType {
		DELETE_ORDER,
		ADD_LIMIT_ORDER,
		UPDATE_LIMIT_ORDER,
		ADD_ICEBERG_ORDER,
		UPDATE_ICEBERG_ORDER,
		ADD_STOP_LIMIT_ORDER,
		UPDATE_STOP_LIMIT_ORDER,
		CHANGE_MATCHING_STATE,
	}

	public ApplicationServiceResponse(ApplicationServiceType type, List<Event> events, BaseRq req) {
		this.type = type;
		this.events = events;
		this.req = req;
	}

	public boolean isTypeDelete() {
		return this.type == ApplicationServiceType.DELETE_ORDER;
	}

	public boolean isTypeChangeState() {
		return this.type == ApplicationServiceType.CHANGE_MATCHING_STATE;
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

	public long getRequestId() throws InvalidRequestFieldAccess {
		if (this.req instanceof BaseOrderRq baseOrderRq) {
			return baseOrderRq.getRequestId();
		}
		throw new InvalidRequestFieldAccess();
	}

	public long getOrderId() throws InvalidRequestFieldAccess {
		if (this.req instanceof BaseOrderRq baseOrderRq) {
			return baseOrderRq.getOrderId();
		}
		throw new InvalidRequestFieldAccess();
	}
}
