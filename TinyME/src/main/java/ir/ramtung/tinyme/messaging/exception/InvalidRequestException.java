package ir.ramtung.tinyme.messaging.exception;

import java.util.List;
import lombok.Getter;
import lombok.ToString;

@ToString
public class InvalidRequestException extends Exception {

	@Getter
	private final List<String> reasons;

	public InvalidRequestException(List<String> reasons) {
		this.reasons = reasons;
	}

	public InvalidRequestException(String reason) {
		this.reasons = List.of(reason);
	}
}
