package ir.ramtung.tinyme.messaging.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import ir.ramtung.tinyme.domain.entity.Side;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class BaseOrderRq extends BaseRq {

	protected long requestId;
	protected Side side;
	protected long orderId;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	protected LocalDateTime entryTime = LocalDateTime.now();
	protected LocalDateTime expiryDate = null;

	protected BaseOrderRq(long requestId, String securityIsin, Side side, long orderId) {
		this.requestId = requestId;
		this.securityIsin = securityIsin;
		this.side = side;
		this.orderId = orderId;
	}

	protected String getAllPropertiesString() {
		return (
			"requestId="    + requestId    + ", " +
			"securityIsin=" + securityIsin + ", " +
			"side="         + side         + ", " +
			"orderId="      + orderId      + ", " +
			"entryTime="    + entryTime
		);
	}

	public abstract List<String> validateYourFields();
}
