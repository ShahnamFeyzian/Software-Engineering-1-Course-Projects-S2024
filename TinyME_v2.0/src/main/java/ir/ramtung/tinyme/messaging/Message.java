package ir.ramtung.tinyme.messaging;

public class Message {
    public static final String INVALID_ORDER_ID = "Invalid order ID";
    public static final String ORDER_QUANTITY_NOT_POSITIVE = "Order quantity is not-positive";
    public static final String ORDER_PRICE_NOT_POSITIVE = "Order price is not-positive";
    public static final String UNKNOWN_SECURITY_ISIN = "Unknown security ISIN";
    public static final String ORDER_ID_NOT_FOUND = "Order ID not found in the order book";
    public static final String INVALID_PEAK_SIZE = "Iceberg order peak size is out of range";
    public static final String CANNOT_SPECIFY_0_PEAK_SIZE_FOR_A_ICEBERG_ORDER = "Cannot specify 0 peak size for a iceberg order";
    public static final String UNKNOWN_BROKER_ID = "Unknown broker ID";
    public static final String UNKNOWN_SHAREHOLDER_ID = "Unknown shareholder ID";
    public static final String BUYER_HAS_NOT_ENOUGH_CREDIT = "Buyer has not enough credit";
    public static final String QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE = "Quantity is not a multiple of security lot size";
    public static final String PRICE_NOT_MULTIPLE_OF_TICK_SIZE = "Price is not a multiple of security tick size";
    public static final String SELLER_HAS_NOT_ENOUGH_POSITIONS = "Seller has not enough positions";
    public static final String INVALID_MINIMUM_EXECUTION_QUANTITY = "Invalid minimum execution quantity";
    public static final String CANNOT_UPDATE_MINIMUM_EXECUTION_QUANTITY = "Cannot update minimum execution quantity";
    public static final String MINIMUM_EXECUTION_QUANTITY_NOT_MET = "The minimum execution quantity was not met";
    public static final String SIDE_CAN_NOT_BE_NULL = "Side can not be null value";
    public static final String INVALID_STOP_PRICE = "Invalid stop price";
    public static final String STOP_LIMIT_ORDERS_CAN_NOT_HAVE_MINIMUM_EXECUTION_QUANTITY = "Stop limit orders can not have minimum execution quantity";
    public static final String STOP_LIMIT_ORDERS_CAN_NOT_BE_ICEBERG = "Stop limit orders can not be iceberg";
    public static final String INVALID_STOP_LIMIT_PRICE = "Given stop limit price is invalid for this order";

}
