package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.domain.exception.NotEnoughPositionException;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.messaging.Message;
import lombok.Builder;
import lombok.Getter;

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

    public MatchResult addNewOrder(Order newOrder, Matcher matcher) {
        try {
            checkPositionForNewOrder(newOrder);
            MatchResult result = matcher.execute(newOrder);
            return result;
        }
        catch (NotEnoughPositionException exp) {
            return MatchResult.notEnoughPositions();
        }
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

    public MatchResult updateOrder(Order tempOrder, Matcher matcher) {
        try {
            Order mainOrder = findByOrderId(tempOrder.getSide(), tempOrder.getOrderId());
            checkPositionForUpdateOrder(mainOrder, tempOrder);
            boolean losesPriority = mainOrder.willPriortyLostInUpdate(tempOrder);
            Order originalOrder = mainOrder.snapshot();
            mainOrder.updateFromTempOrder(tempOrder);
            if (losesPriority) 
                return reAddUpdatedOrder(mainOrder, originalOrder, matcher);
            else
                return MatchResult.executed(null, List.of());
        }
        catch (NotEnoughPositionException exp) {
            return MatchResult.notEnoughPositions();
        }
    }

    private MatchResult reAddUpdatedOrder(Order updatOrder, Order originalOrder, Matcher matcher) {
        orderBook.removeByOrderId(originalOrder.getSide(), originalOrder.getOrderId());
        MatchResult matchResult = matcher.execute(updatOrder);
        if (matchResult.outcome() != MatchingOutcome.EXECUTED) {
            orderBook.enqueue(originalOrder);
            if (originalOrder.getSide() == Side.BUY) {
                originalOrder.getBroker().decreaseCreditBy(originalOrder.getValue());
            }
        }
        return matchResult;
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

    public Order findByOrderId(Side side, long orderId) {
        return orderBook.findByOrderId(side, orderId);
    }

    public boolean isThereOrderWithId(Side side, long orderId) {
        return orderBook.isThereOrderWithId(side, orderId);
    }
}
