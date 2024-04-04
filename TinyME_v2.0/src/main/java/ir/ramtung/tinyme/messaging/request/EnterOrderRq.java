package ir.ramtung.tinyme.messaging.request;

import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.messaging.Message;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Getter
public class EnterOrderRq extends BaseOrder{
    private OrderEntryType requestType;
    private int quantity;
    private int price;
    private long brokerId;
    private long shareholderId;
    private int peakSize;
    private int minimumExecutionQuantity;
    // TODO 
    // why update(amend) order should have brokerId and shareholder ?
    // the orderId isn't enough ?

    private EnterOrderRq(OrderEntryType orderEntryType, long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize) {
        super(requestId, securityIsin, side, orderId);
        this.requestType = orderEntryType;
        this.entryTime = entryTime;
        this.quantity = quantity;
        this.price = price;
        this.brokerId = brokerId;
        this.shareholderId = shareholderId;
        this.peakSize = peakSize;
    }

    public static EnterOrderRq createNewOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize) {
        return new EnterOrderRq(OrderEntryType.NEW_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize);
    }

    public static EnterOrderRq createUpdateOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize) {
        return new EnterOrderRq(OrderEntryType.UPDATE_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize);
    }

    @Override
    public List<String> validateYourFields() {
        List<String> errors = new LinkedList<>();
        if (orderId <= 0)
            errors.add(Message.INVALID_ORDER_ID);
        if (quantity <= 0)
            errors.add(Message.ORDER_QUANTITY_NOT_POSITIVE);
        if (price <= 0)
            errors.add(Message.ORDER_PRICE_NOT_POSITIVE);
        if (!isPeakSizeValid())
            errors.add(Message.INVALID_PEAK_SIZE);

        return errors;
    }

    public boolean isPeakSizeValid() {
        return (peakSize >= 0) && (peakSize < quantity);
        //TODO 
        // peakSize <= quantity ??
    }

}
