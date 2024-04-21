package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import ir.ramtung.tinyme.domain.exception.NotFoundException;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.domain.exception.NotEnoughPositionException;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.messaging.Message;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Getter
@Builder
public class Security {
    private String isin;
    @Builder.Default
    private int tickSize = 1;
    @Builder.Default
    private int lotSize = 1;
    @Builder.Default
    private OrderBook orderBook = new OrderBook();
    private int lastTradePrice;

    public List<MatchResult> addNewOrder(Order newOrder, Matcher matcher) {
        try {
            List<MatchResult> results = new ArrayList<>();
            checkPositionForNewOrder(newOrder);

            if (newOrder instanceof StopLimitOrder newStopLimitOrder) {
                addNewStopLimitOrder(newStopLimitOrder);
                results.addFirst(MatchResult.executed(newOrder, List.of()));
            }
            else {
                MatchResult newOrderMatchResult = matcher.execute(newOrder);
                updateLastTradePrice(newOrderMatchResult.trades());
                results.addFirst(newOrderMatchResult);
            }

            results.addAll(executeStopLimitOrders(matcher));
            return results;
        }
        catch (NotEnoughPositionException exp) {
            return List.of(MatchResult.notEnoughPositions());
        }
        catch (NotEnoughCreditException exp) {
            return List.of(MatchResult.notEnoughCredit());
        }
    }

    private void updateLastTradePrice(List<Trade> trades) {
        if (!trades.isEmpty())
            lastTradePrice = trades.getLast().getPrice();
    }

    private void addNewStopLimitOrder(StopLimitOrder newOrder) {
        orderBook.enqueueStopLimitOrder(newOrder);
    }

    private void checkPositionForNewOrder(Order newOrder) {
        if (newOrder.getSide() == Side.BUY)
            return;
        
        Shareholder shareholder = newOrder.getShareholder();
        int salesAmount = newOrder.getQuantity();
        int queuedPositionAmount = orderBook.totalSellQuantityByShareholder(shareholder);
        int totalNeededPosition = salesAmount + queuedPositionAmount;
        if (!shareholder.hasEnoughPositionsOn(this, totalNeededPosition)) 
            throw new NotEnoughPositionException();
    }

    public void deleteOrder(Side side, long orderId) {
        orderBook.removeByOrderId(side, orderId);
    }

    // DUP
    public List<MatchResult> updateSloOrder(StopLimitOrder tempOrder, Matcher matcher) {
        try {
            StopLimitOrder mainOrder = (StopLimitOrder) findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
            checkPositionForUpdateOrder(mainOrder, tempOrder);
            StopLimitOrder originalOrder = mainOrder.snapshot();
            orderBook.removeByOrderId(originalOrder.getSide(), originalOrder.getOrderId());
            mainOrder.updateFromTempSloOrder(tempOrder);
            return reAddUpdatedSloOrder(mainOrder, originalOrder, matcher);

        }
        catch (NotEnoughPositionException exp) {
            return List.of(MatchResult.notEnoughPositions());
        }
    }
    // DUP
    private List<MatchResult> reAddUpdatedSloOrder(StopLimitOrder updatedOrder, StopLimitOrder originalOrder, Matcher matcher) {
        try {
            List<MatchResult> results = new LinkedList<>();
            results.add(MatchResult.executed(updatedOrder, List.of()));
            addNewStopLimitOrder(updatedOrder);
            results.addAll(executeStopLimitOrders(matcher));
            return results;
        }
        catch (NotEnoughCreditException exp) {
            addNewStopLimitOrder(originalOrder);
            return List.of(MatchResult.notEnoughCredit());
        }
    }

    public List<MatchResult> updateOrder(Order tempOrder, Matcher matcher) {
        try {
            Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
            checkPositionForUpdateOrder(mainOrder, tempOrder);
            boolean losesPriority = mainOrder.willPriortyLostInUpdate(tempOrder);
            if (losesPriority) {
                Order originalOrder = mainOrder.snapshot();
                orderBook.removeByOrderId(originalOrder.getSide(), originalOrder.getOrderId());
                mainOrder.updateFromTempOrder(tempOrder);
                return reAddUpdatedOrder(mainOrder, originalOrder, matcher);
            }
            else {
                mainOrder.updateFromTempOrder(tempOrder);
                return List.of(MatchResult.executed(null, List.of()));
            }
        }
        catch (NotEnoughPositionException exp) {
            return List.of(MatchResult.notEnoughPositions());
        }
    }

    private List<MatchResult> reAddUpdatedOrder(Order updatedOrder, Order originalOrder, Matcher matcher) {
        MatchResult updatedOrderResult = matcher.execute(updatedOrder);
        if (updatedOrderResult.outcome() != MatchingOutcome.EXECUTED) {
            orderBook.enqueue(originalOrder);
        }
        updateLastTradePrice(updatedOrderResult.trades());
        List<MatchResult> results = executeStopLimitOrders(matcher);
        results.addFirst(updatedOrderResult);
        return results;
        // TODO
        // this is just painkiller, it should be treated properly
    }

    private void checkPositionForUpdateOrder(Order mainOrder, Order tempOrder) {
        if (mainOrder.getSide() == Side.BUY)
            return;
        
        Shareholder shareholder = mainOrder.getShareholder();
        int pervSalesAmount = mainOrder.getTotalQuantity();
        int newSalesAmount = tempOrder.getTotalQuantity();
        int queuedPositionAmount = orderBook.totalSellQuantityByShareholder(shareholder);
        int totalNeededPosition = newSalesAmount + queuedPositionAmount - pervSalesAmount;
        if (!shareholder.hasEnoughPositionsOn(this, totalNeededPosition)) 
            throw new NotEnoughPositionException();
    }

    public List<String> checkLotAndTickSize(EnterOrderRq order) {
        List<String> errors = new LinkedList<>();
        if (order.getQuantity() % lotSize != 0)
            errors.add(Message.QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE);
        if (order.getPrice() % tickSize != 0)
            errors.add(Message.PRICE_NOT_MULTIPLE_OF_TICK_SIZE);
    
        return errors;
    }

    private List<MatchResult> executeStopLimitOrders(Matcher matcher) {
        List<MatchResult> results = new LinkedList<>();
        StopLimitOrder sloOrder;
        while((sloOrder = orderBook.getStopLimitOrder(lastTradePrice)) != null) {
            Order activedOrder = new Order(sloOrder);
            MatchResult result = matcher.execute(activedOrder);
            updateLastTradePrice(result.trades());
            results.add(result);
        }
        return results;
    }

    // DUP
    public Order findByOrderId(Side side, long orderId) {
        try {
            return orderBook.findByOrderId(side, orderId);
        }
        catch (NotFoundException exp) {
            return orderBook.findBySloOrderId(side, orderId);
        }

    }

    // DUP
    public boolean isThereOrderWithId(Side side, long orderId) {
        return (orderBook.isThereOrderWithId(side, orderId) || orderBook.isThereSloOrderWithId(side, orderId));
    }
}
