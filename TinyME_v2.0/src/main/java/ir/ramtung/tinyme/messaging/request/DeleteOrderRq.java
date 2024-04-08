package ir.ramtung.tinyme.messaging.request;

import java.util.LinkedList;
import java.util.List;

import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.messaging.Message;

public class DeleteOrderRq extends BaseOrder{

    public DeleteOrderRq(long requestId, String securityIsin, Side side, long orderId) {
        super(requestId, securityIsin, side, orderId);
    }

    @Override
    public List<String> validateYourFields() {
        List<String> errors = new LinkedList<>();
        if (orderId <= 0)
            errors.add(Message.INVALID_ORDER_ID);
        if(side == null)
            errors.add(Message.SIDE_CAN_NOT_BE_NULL);

        return errors;
    }
}
