package ir.ramtung.tinyme.messaging.event;

import ir.ramtung.tinyme.messaging.TradeDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class OrderExecutedEvent extends Event {

	private long requestId;
	private long orderId;
	private List<TradeDTO> trades;

	public OrderExecutedEvent(long orderId, List<TradeDTO> trades) {
		this.orderId = orderId;
		this.trades = trades;
	}
}
