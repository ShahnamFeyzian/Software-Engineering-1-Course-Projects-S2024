package ir.ramtung.tinyme.domain.service.controls;

import org.springframework.stereotype.Service;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.domain.entity.Order;

@Service
public class CreditControl {
    public ControlResult chekCreditForContinousMatching(Order newOrder, Order matchingOrder) {
        if (newOrder.isSell()) {
            return ControlResult.OK;
        }
        
        int quantity = Math.min(newOrder.getQuantity(), matchingOrder.getQuantity());
        int value = quantity * matchingOrder.getPrice();
        Broker broker = newOrder.getBroker();
        
        if (broker.hasEnoughCredit(value)) {
            return ControlResult.OK;
        }
        else {
            return ControlResult.NOT_ENOUGH_CREDIT;
        }
    }
}
