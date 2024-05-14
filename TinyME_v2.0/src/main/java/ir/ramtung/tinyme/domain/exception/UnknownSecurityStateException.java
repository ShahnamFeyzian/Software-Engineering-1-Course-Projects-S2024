package ir.ramtung.tinyme.domain.exception;

public class UnknownSecurityStateException extends RuntimeException{
    public UnknownSecurityStateException(String msg) {
		super(msg);
	}

	public UnknownSecurityStateException() {
		super();
	}
}
