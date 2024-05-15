package ir.ramtung.tinyme.messaging.event;

import ir.ramtung.tinyme.domain.entity.Trade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class TradeEvent extends Event {
    private String securityIsin;
    private int price;
    private int quantity;
    private long buyId;
    private long sellId;

    public TradeEvent(Trade trade) {
        super();
        this.securityIsin = trade.getSecurity().getIsin();
        this.price = trade.getPrice();
        this.quantity = trade.getQuantity();
        this.buyId = trade.getBuy().getOrderId();
        this.sellId = trade.getSell().getOrderId();
    }
}
