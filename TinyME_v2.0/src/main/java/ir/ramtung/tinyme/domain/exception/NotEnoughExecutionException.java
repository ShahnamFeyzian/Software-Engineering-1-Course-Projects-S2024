package ir.ramtung.tinyme.domain.exception;

public class NotEnoughExecutionException extends RuntimeException {

	public NotEnoughExecutionException(String msg) {
		super(msg);
	}

	public NotEnoughExecutionException() {
		super();
	}
}
