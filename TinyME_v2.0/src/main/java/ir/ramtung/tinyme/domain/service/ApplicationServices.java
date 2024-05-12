package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.exception.InvalidIcebergPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidPeakSizeException;
import ir.ramtung.tinyme.domain.exception.InvalidStopLimitPriceException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.domain.exception.UpdateMinimumExecutionQuantityException;
import ir.ramtung.tinyme.domain.service.ApplicationServiceResponse.ApplicationServiceType;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.ChangeMatchingStateRq;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import java.util.List;
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

	public ApplicationServiceResponse deleteOrder(DeleteOrderRq req) {
		validateDeleteOrderRq(req);
		setEntitiesByRq(req);
		security.deleteOrder(req.getSide(), req.getOrderId());

		return new ApplicationServiceResponse(ApplicationServiceType.DELETE_ORDER, null, req);
	}

	public void validateChangeMatchingState(ChangeMatchingStateRq req) {
		try {
			securityRepository.findSecurityByIsin(req.getSecurityIsin());
		} catch (NotFoundException exp) {
			throw new InvalidRequestException(Message.UNKNOWN_SECURITY_ISIN);
		}
	}

	public ApplicationServiceResponse changeMatchingState(ChangeMatchingStateRq req) {
		validateChangeMatchingState(req);
		setEntitiesByRq(req);
		security.changeMatchingState(req.getTargetState());

		return new ApplicationServiceResponse(ApplicationServiceType.CHANGE_MATCHING_STATE, null, req);
	}

	public ApplicationServiceResponse addLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		Order tempOrder = Order.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.addNewOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_LIMIT_ORDER, results, req);
	}

	public ApplicationServiceResponse updateLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		Order tempOrder = Order.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.updateOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_LIMIT_ORDER, results, req);
	}

	public ApplicationServiceResponse addIcebergOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		IcebergOrder tempOrder = IcebergOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.addNewOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_ICEBERG_ORDER, results, req);
	}

	public ApplicationServiceResponse updateIcebergOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		IcebergOrder tempOrder = IcebergOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.updateOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_ICEBERG_ORDER, results, req);
	}

	public ApplicationServiceResponse addStopLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		setEntitiesByRq(req);
		StopLimitOrder tempOrder = StopLimitOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.addNewOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.ADD_STOP_LIMIT_ORDER, results, req);
	}

	public ApplicationServiceResponse updateStopLimitOrder(EnterOrderRq req) {
		generalEnterOrderValidation(req);
		validateUpdateOrderRq(req);
		setEntitiesByRq(req);
		StopLimitOrder tempOrder = StopLimitOrder.createTempOrderByEnterRq(security, broker, shareholder, req);
		List<MatchResult> results = security.updateOrder(tempOrder);

		return new ApplicationServiceResponse(ApplicationServiceType.UPDATE_STOP_LIMIT_ORDER, results, req);
	}
}
