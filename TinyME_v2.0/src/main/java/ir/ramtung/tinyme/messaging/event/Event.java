package ir.ramtung.tinyme.messaging.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public abstract class Event {

	@Getter
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private final LocalDateTime time;

	public Event() {
		time = LocalDateTime.now();
	}

	public Event(LocalDateTime time) {
		this.time = time;
	}
}
