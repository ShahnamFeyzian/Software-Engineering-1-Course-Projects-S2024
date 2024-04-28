package ir.ramtung.tinyme.domain.exception;

public class InvalidPeakSizeException extends RuntimeException {

	public InvalidPeakSizeException(String msg) {
		super(msg);
	}

	public InvalidPeakSizeException() {
		super();
	}
}
