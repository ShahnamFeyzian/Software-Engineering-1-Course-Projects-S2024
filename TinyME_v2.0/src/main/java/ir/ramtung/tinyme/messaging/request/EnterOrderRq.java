package ir.ramtung.tinyme.messaging.request;

import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.messaging.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Getter
@NoArgsConstructor
public class EnterOrderRq extends BaseOrderRq{
    private OrderEntryType requestType;
    private int quantity;
    private int price;
    private long brokerId;
    private long shareholderId;
    private int peakSize;
    private int minimumExecutionQuantity;
    private int stopPrice;

    private EnterOrderRq(OrderEntryType orderEntryType, long requestId, String securityIsin, long orderId,
    LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, 
    int minimumExecutionQuantity, int stopPrice) {
        super(requestId, securityIsin, side, orderId);
        this.requestType = orderEntryType;
        this.entryTime = entryTime;
        this.quantity = quantity;
        this.price = price;
        this.brokerId = brokerId;
        this.shareholderId = shareholderId;
        this.peakSize = peakSize;
        this.minimumExecutionQuantity = minimumExecutionQuantity;
        this.stopPrice = stopPrice;
    }

    public static EnterOrderRq createNewOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, 
    Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity) {
        return new EnterOrderRq(OrderEntryType.NEW_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, 
                                brokerId, shareholderId, peakSize, minimumExecutionQuantity, 0);
    }

    public static EnterOrderRq createUpdateOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, 
    Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity) {
        return new EnterOrderRq(OrderEntryType.UPDATE_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, 
                                brokerId, shareholderId, peakSize, minimumExecutionQuantity, 0);
    }

    public static EnterOrderRq createNewOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, 
    Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity, int stopPrice) {
        return new EnterOrderRq(OrderEntryType.NEW_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, 
                                brokerId, shareholderId, peakSize, minimumExecutionQuantity, stopPrice);
    }

    public static EnterOrderRq createUpdateOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, 
    Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity, int stopPrice) {
        return new EnterOrderRq(OrderEntryType.UPDATE_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, 
                                brokerId, shareholderId, peakSize, minimumExecutionQuantity, stopPrice);
    }

    @Override
    public String toString() {
        return "EnterOrderRq(" + this.getAllPropertiesString() + ")";
    }

    @Override
    protected String getAllPropertiesString() {
        return (
            super.getAllPropertiesString() + ", " +
            "requestType=" + requestType + ", " +
            "quantity=" + quantity + ", " +
            "price=" + price + ", " +
            "brokerId=" + brokerId + ", " +
            "shareholderId=" + shareholderId + ", " +
            "peakSize=" + peakSize + ", " +
            "minimumExecutionQuantity=" + minimumExecutionQuantity
        );
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
        if (!isMinimumExecutionQuantityValid())
            errors.add(Message.INVALID_MINIMUM_EXECUTION_QUANTITY);
        if (this.side == null)
            errors.add(Message.SIDE_CAN_NOT_BE_NULL);
        errors.addAll(stopPriceValidation());
        return errors;
    }

    private List<String> stopPriceValidation() {
        List<String> errors = new LinkedList<>();
        if (this.stopPrice == 0)
            return errors;

        if (this.stopPrice < 0)
            errors.add(Message.INVALID_STOP_PRICE);
        if (this.minimumExecutionQuantity != 0)
            errors.add(Message.STOP_LIMIT_ORDERS_CAN_NOT_HAVE_MINIMUM_EXECUTION_QUANTITY);
        if (this.peakSize != 0)
            errors.add(Message.STOP_LIMIT_ORDERS_CAN_NOT_BE_ICEBERG);
        return errors;
    }

    private boolean isPeakSizeValid() {
        return (peakSize >= 0) && (peakSize < quantity);
    }

    private boolean isMinimumExecutionQuantityValid() {
        return (minimumExecutionQuantity >= 0) && (minimumExecutionQuantity <= quantity);
    }
}
