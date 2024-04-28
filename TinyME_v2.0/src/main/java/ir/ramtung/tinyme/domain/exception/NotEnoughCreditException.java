package ir.ramtung.tinyme.domain.exception;

import lombok.ToString;

@ToString
public class NotEnoughCreditException extends RuntimeException {

	public NotEnoughCreditException(String msg) {
		super(msg);
	}

	public NotEnoughCreditException() {
		super();
	}
}
