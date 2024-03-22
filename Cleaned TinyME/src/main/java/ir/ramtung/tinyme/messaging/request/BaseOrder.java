package ir.ramtung.tinyme.messaging.request;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import ir.ramtung.tinyme.domain.entity.Side;
import lombok.Getter;

@Getter
public abstract class BaseOrder {
    protected long requestId;
    protected String securityIsin;
    protected Side side;
    protected long orderId;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    protected LocalDateTime entryTime;

    protected BaseOrder(long requestId, String securityIsin, Side side, long orderId) {
        this.requestId = requestId;
        this.securityIsin = securityIsin;
        this.side = side;
        this.orderId = orderId;
        this.entryTime = LocalDateTime.now();
    }
    
    public abstract List<String> validateYourFields();
}
