package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.entity.security_stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SecurityStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SituationalStats;
import ir.ramtung.tinyme.domain.entity.security_stats.SituationalStatsType;
import ir.ramtung.tinyme.domain.exception.InvalidIcebergPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import ir.ramtung.tinyme.domain.service.ApplicationServiceResponse.ApplicationServiceType;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.Event;
import ir.ramtung.tinyme.messaging.event.OrderAcceptedEvent;
import ir.ramtung.tinyme.messaging.event.OrderActivatedEvent;
import ir.ramtung.tinyme.messaging.event.OrderDeletedEvent;
import ir.ramtung.tinyme.messaging.event.OrderExecutedEvent;
import ir.ramtung.tinyme.messaging.event.OrderRejectedEvent;
import ir.ramtung.tinyme.messaging.event.OrderUpdatedEvent;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.ChangeMatchingStateRq;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.messaging.request.MatchingState;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ApplicationServices {

	private SecurityRepository securityRepository;
	private BrokerRepository brokerRepository;
	private ShareholderRepository shareholderRepository;
	private Security security;
	private Broker broker;
	private Shareholder shareholder;

	public ApplicationServices(
		SecurityRepository securityRepository,
		BrokerRepository brokerRepository,
		ShareholderRepository shareholderRepository
	) {
		this.brokerRepository = brokerRepository;
		this.shareholderRepository = shareholderRepository;
		this.securityRepository = securityRepository;
	}

	private void setEntitiesByRq(EnterOrderRq req) {
		this.security = securityRepository.findSecurityByIsin(req.getSecurityIsin());
		this.broker = brokerRepository.findBrokerById(req.getBrokerId());
		this.shareholder = shareholderRepository.findShareholderById(req.getShareholderId());
	}

	private void setEntitiesByRq(DeleteOrderRq req) {
		this.security = securityRepository.findSecurityByIsin(req.getSecurityIsin());
	}

	private void setEntitiesByRq(ChangeMatchingStateRq req) {
		this.security = securityRepository.findSecurityByIsin(req.getSecurityIsin());
	}

	private void validateDeleteOrderRq(DeleteOrderRq deleteOrderRq) {
		List<String> errors = deleteOrderRq.validateYourFields();

		if (!securityRepository.isThereSecurityWithIsin(deleteOrderRq.getSecurityIsin())) {
			errors.add(Message.UNKNOWN_SECURITY_ISIN);
		}

		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}

		Security security = securityRepository.findSecurityByIsin(deleteOrderRq.getSecurityIsin());
		if (!security.isThereOrderWithId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId())) {
			throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
		}
	}

	private void generalEnterOrderValidation(EnterOrderRq enterOrderRq) {
		List<String> errors = enterOrderRq.validateYourFields();
		if (!securityRepository.isThereSecurityWithIsin(enterOrderRq.getSecurityIsin())) {
			errors.add(Message.UNKNOWN_SECURITY_ISIN);
		} else {
			Security security = securityRepository.findSecurityByIsin(enterOrderRq.getSecurityIsin());
			errors.addAll(security.checkEnterOrderRq(enterOrderRq));
		}

		if (!brokerRepository.isThereBrokerWithId(enterOrderRq.getBrokerId())) {
			errors.add(Message.UNKNOWN_BROKER_ID);
		}

		if (!shareholderRepository.isThereShareholderWithId(enterOrderRq.getShareholderId())) {
			errors.add(Message.UNKNOWN_SHAREHOLDER_ID);
		}

		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	private void validateUpdateOrderRq(EnterOrderRq updateOrderRq) {
		try {
			Security security = securityRepository.findSecurityByIsin(updateOrderRq.getSecurityIsin());
			Order order = security.findByOrderId(updateOrderRq.getSide(), updateOrderRq.getOrderId());
			order.checkNewPeakSize(updateOrderRq.getPeakSize());
			order.checkNewMinimumExecutionQuantity(updateOrderRq.getMinimumExecutionQuantity());
			order.checkNewStopLimitPrice(updateOrderRq.getStopPrice());
		} catch (NotFoundException exp) {
			throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
		} catch (InvalidIcebergPeakSizeException exp) {
			throw new InvalidRequestException(Message.CANNOT_SPECIFY_0_PEAK_SIZE_FOR_A_ICEBERG_ORDER);
		} catch (InvalidPeakSizeException exp) {
			throw new InvalidRequestException(Message.INVALID_PEAK_SIZE);
		} catch (UpdateMinimumExecutionQuantityException exp) {
			throw new InvalidRequestException(Message.CANNOT_UPDATE_MINIMUM_EXECUTION_QUANTITY);
		} catch (InvalidStopLimitPriceException exp) {
			throw new InvalidRequestException(Message.INVALID_STOP_LIMIT_UPDATE_PRICE);
		}
	}

	private void validateChangeMatchingState(ChangeMatchingStateRq req) {
		try {
			securityRepository.findSecurityByIsin(req.getSecurityIsin());
		} catch (NotFoundException exp) {
			throw new InvalidRequestException(Message.UNKNOWN_SECURITY_ISIN);
		}
	}

	private List<Event> createEventsForOrderRequest(List<SecurityStats> securityStats, long requestId) {
		List<Event> events = new ArrayList<>();
		for (SecurityStats stats : securityStats) {
			if (stats instanceof SituationalStats situationalStats) {
				events.add(createEventFromSituationalStats(situationalStats, requestId));
			} else if (stats instanceof ExecuteStats executeStats) {
				events.add(createEventFromExecuteStats(executeStats, requestId));
			}
		}
		return events;
	}

	private Event createEventFromSituationalStats(SituationalStats situationalStats, long requestId) {
		long orderId = situationalStats.getOrderId();
		switch (situationalStats.getType()) {
            case SituationalStatsType.DELETE_ORDER         : return new OrderDeletedEvent(requestId, orderId); 
            case SituationalStatsType.ADD_ORDER            : return new OrderAcceptedEvent(requestId, orderId);
            case SituationalStatsType.UPDATE_ORDER         : return new OrderUpdatedEvent(requestId, orderId);
            case SituationalStatsType.ORDER_ACTIVATED      : return new OrderActivatedEvent(orderId);
            case SituationalStatsType.NOT_ENOUGH_CREDIT    : return new OrderRejectedEvent(requestId, orderId, List.of(Message.BUYER_HAS_NOT_ENOUGH_CREDIT));
            case SituationalStatsType.NOT_ENOUGH_POSITIONS : return new OrderRejectedEvent(requestId, orderId, List.of(Message.SELLER_HAS_NOT_ENOUGH_POSITIONS));
            case SituationalStatsType.NOT_ENOUGH_EXECUTION : return new OrderRejectedEvent(requestId, orderId, List.of(Message.MINIMUM_EXECUTION_QUANTITY_NOT_MET));
            default : throw new UnknownError("Unknown SituationalStatsType");
        }
	}

	private Event createEventFromExecuteStats(ExecuteStats executeStats, long requestId) {
		long orderId = executeStats.getOrderId();
		if (executeStats.isCountinues()) {
			return new OrderExecutedEvent(requestId, orderId, executeStats.getTrades().stream().map(TradeDTO::new).collect(Collectors.toList()));
		} else {
			// TODO: complete this part
			return null;
		}
	}

	public ApplicationServiceResponse deleteOrder(DeleteOrderRq req) {
		validateDeleteOrderRq(req);
		setEntitiesByRq(req);
		SecurityResponse response = security.deleteOrder(req.getSide(), req.getOrderId());
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.DELETE_ORDER, events, req);
	}

	public ApplicationServiceResponse addLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		Order tempOrder = Order.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.addNewOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_LIMIT_ORDER, events, req);
	}

	public ApplicationServiceResponse updateLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		Order tempOrder = Order.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.updateOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_LIMIT_ORDER, events, req);
	}

	public ApplicationServiceResponse addIcebergOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		IcebergOrder tempOrder = IcebergOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.addNewOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_ICEBERG_ORDER, events, req);
	}

	public ApplicationServiceResponse updateIcebergOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		IcebergOrder tempOrder = IcebergOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.updateOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_ICEBERG_ORDER, events, req);
	}

	public ApplicationServiceResponse addStopLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		StopLimitOrder tempOrder = StopLimitOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.addNewOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_STOP_LIMIT_ORDER, events, req);
	}

	public ApplicationServiceResponse updateStopLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		StopLimitOrder tempOrder = StopLimitOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		SecurityResponse response = security.updateOrder(tempOrder);
		List<Event> events = createEventsForOrderRequest(response.getStats(), req.getRequestId());

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_STOP_LIMIT_ORDER, events, req);
	}

	public ApplicationServiceResponse changeMatchingState(ChangeMatchingStateRq req) {
		validateChangeMatchingState(req);
		setEntitiesByRq(req);
		SecurityState targetSecurityState = (req.getTargetState() == MatchingState.AUCTION) ? SecurityState.AUCTION : SecurityState.CONTINUOUES;
		security.changeMatchingState(targetSecurityState);
		// TODO: complete this part

		return new ApplicationServiceResponse(ApplicationServiceType.CHANGE_MATCHING_STATE, null, req);
	}
}
