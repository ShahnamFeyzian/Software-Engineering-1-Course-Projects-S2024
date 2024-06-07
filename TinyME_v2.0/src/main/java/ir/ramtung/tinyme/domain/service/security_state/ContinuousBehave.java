package ir.ramtung.tinyme.domain.service.security_state;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.MatchResult;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.OrderBook;
import ir.ramtung.tinyme.domain.entity.SecurityState;
import ir.ramtung.tinyme.domain.entity.StopLimitOrder;
import ir.ramtung.tinyme.domain.entity.stats.ExecuteStats;
import ir.ramtung.tinyme.domain.entity.stats.SecurityStats;
import ir.ramtung.tinyme.domain.entity.stats.SituationalStats;
import ir.ramtung.tinyme.domain.entity.stats.StateStats;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.controls.ControlResult;
import ir.ramtung.tinyme.domain.service.controls.CreditControl;
import ir.ramtung.tinyme.domain.service.controls.PositionControl;

@Service
public class ContinuousBehave implements SecurityBehave {
	private PositionControl positionControl;
	private CreditControl creditControl;
	private Matcher matcher;

    public ContinuousBehave(PositionControl positionControl, CreditControl creditControl, Matcher matcher) {
        this.positionControl = positionControl;
        this.creditControl = creditControl;
        this.matcher = matcher;
    }

    @Override
    public List<SecurityStats> addNewOrder(Order newOrder, OrderBook orderBook, int lastTradePrice) {
        if (positionControl.checkPositionForOrder(newOrder, orderBook) != ControlResult.OK) {
			return List.of(SituationalStats.createNotEnoughPositionsStats(newOrder.getOrderId()));
		}

        if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
			return addNewStopLimitOrder(newStopLimitOrder, orderBook);
		} else {
			return addNewLimitOrder(newOrder, orderBook);
		}
    }

    @Override
    public List<SecurityStats> updateOrder(Order tempOrder, Order mainOrder, OrderBook orderBook, int lastTradePrice) {
        boolean losesPriority = mainOrder.willPriorityLostInUpdate(tempOrder);
		if (losesPriority) {
			Order originalOrder = mainOrder.snapshot();
			creditControl.updateCreditAtDelete(mainOrder);
			orderBook.removeOrder(mainOrder);
			mainOrder.updateFromTempOrder(tempOrder);
			return reAddUpdatedOrder(mainOrder, originalOrder, orderBook);
		} else {
			mainOrder.updateFromTempOrder(tempOrder);
		    return List.of(SituationalStats.createUpdateOrderStats(mainOrder.getOrderId()));
		}
    }

    @Override
    public List<SecurityStats> deleteOrder(Order targetOrder, OrderBook orderBook, int lastTradePrice) {
        creditControl.updateCreditAtDelete(targetOrder);
		orderBook.removeOrder(targetOrder);
        return List.of(SituationalStats.createDeleteOrderStats(targetOrder.getOrderId()));
    }

    @Override
    public List<SecurityStats> activateStopLimitOrders(OrderBook orderBook, int lastTradePrice) {
        List<SecurityStats> stats = new LinkedList<>();
		StopLimitOrder slo;
        int currentLastTradePrice = lastTradePrice;

		while ((slo = orderBook.getStopLimitOrder(currentLastTradePrice)) != null) {
			creditControl.updateCreditAtDelete(slo);
			stats.add(SituationalStats.createOrderActivatedStats(slo.getOrderId(), slo.getRequestId()));
			Order activatedOrder = new Order(slo);
            MatchResult result = matcher.continuousExecuting(activatedOrder, orderBook);
            if (!result.trades().isEmpty()) {
                stats.add(ExecuteStats.createContinuousExecuteStatsForActivatedOrder(result.trades(), activatedOrder.getOrderId(), slo.getRequestId()));
                currentLastTradePrice = result.trades().getLast().getPrice();
            }
		}

		return stats;
    }

    @Override
    public List<SecurityStats> changeMatchingState(OrderBook orderBook, int lastTradePrice, SecurityState newState) {
        return List.of(StateStats.createStateStats(SecurityState.CONTINUOUS, newState));
    }
    
    private List<SecurityStats> addNewStopLimitOrder(StopLimitOrder newOrder, OrderBook orderBook) {
		if (creditControl.checkCreditForBeingQueued(newOrder) != ControlResult.OK) {
			return List.of(SituationalStats.createNotEnoughCreditStats(newOrder.getOrderId()));
		}

		creditControl.updateCreditForBeingQueued(newOrder);
		orderBook.enqueue(newOrder);
		return List.of(SituationalStats.createAddOrderStats(newOrder.getOrderId()));
	}

    private List<SecurityStats> addNewLimitOrder(Order newOrder, OrderBook orderBook) {
		List<SecurityStats> stats = new ArrayList<>();
		stats.add(SituationalStats.createAddOrderStats(newOrder.getOrderId()));

		MatchResult newOrderMatchResult = matcher.continuousExecuting(newOrder, orderBook);
		if (!newOrderMatchResult.isSuccessful()) {
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(newOrderMatchResult, newOrder.getOrderId()));
		}
		if(!newOrderMatchResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuousExecuteStats(newOrderMatchResult.trades(), newOrder.getOrderId()));
		}
		return stats;
	}

    private List<SecurityStats> reAddUpdatedOrder(Order updatedOrder, Order originalOrder, OrderBook orderBook) {
		if (positionControl.checkPositionForOrder(updatedOrder, orderBook) != ControlResult.OK) {
			creditControl.updateCreditForBeingQueued(originalOrder);
			orderBook.enqueue(originalOrder);
			return List.of(SituationalStats.createNotEnoughPositionsStats(originalOrder.getOrderId()));
		}
		
		if (updatedOrder instanceof StopLimitOrder updatedSlo) {
			StopLimitOrder originalSlo = (StopLimitOrder) originalOrder;
			return reAddUpdatedSlo(updatedSlo, originalSlo, orderBook);
		} else {
			return reAddActiveOrder(updatedOrder, originalOrder, orderBook);
		}
	}

    private List<SecurityStats> reAddUpdatedSlo(StopLimitOrder updatedOrder,StopLimitOrder originalOrder, OrderBook orderBook) {
			if (creditControl.checkCreditForBeingQueued(updatedOrder) != ControlResult.OK) {
				creditControl.updateCreditForBeingQueued(originalOrder);
				orderBook.enqueue(originalOrder);
				return List.of(SituationalStats.createNotEnoughCreditStats(originalOrder.getOrderId()));
			}

			creditControl.updateCreditForBeingQueued(updatedOrder);
			orderBook.enqueue(updatedOrder);

			List<SecurityStats> stats = new LinkedList<>();
			stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
			return stats;
	}

    private List<SecurityStats> reAddActiveOrder(Order updatedOrder, Order originalOrder, OrderBook orderBook) {
		List<SecurityStats> stats = new LinkedList<>();
		stats.add(SituationalStats.createUpdateOrderStats(originalOrder.getOrderId()));
		
		MatchResult updatedOrderResult = matcher.continuousExecuting(updatedOrder, orderBook);

		if (!updatedOrderResult.isSuccessful()) {
			creditControl.updateCreditForBeingQueued(originalOrder);
			orderBook.enqueue(originalOrder);
			stats.set(0, SituationalStats.createExecutionStatsFromUnsuccessfulMatchResult(updatedOrderResult, originalOrder.getOrderId()));
		} 
		if (!updatedOrderResult.trades().isEmpty()) {
			stats.add(ExecuteStats.createContinuousExecuteStats(updatedOrderResult.trades(), originalOrder.getOrderId()));
		}

		return stats;
	}
}
