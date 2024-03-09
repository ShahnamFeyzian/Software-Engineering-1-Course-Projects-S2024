package ir.ramtung.tinyme.messaging.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class OrderRejectedEvent extends Event {

	private long requestId;
	private long orderId;
	private List<String> errors;
}
