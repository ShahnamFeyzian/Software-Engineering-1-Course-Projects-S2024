package ir.ramtung.tinyme.domain.exception;

public class InvalidRequestFieldAccess extends RuntimeException {

	public InvalidRequestFieldAccess() {
		super("Invalid request field access");
	}

	public InvalidRequestFieldAccess(String message) {
		super(message);
	}
}
