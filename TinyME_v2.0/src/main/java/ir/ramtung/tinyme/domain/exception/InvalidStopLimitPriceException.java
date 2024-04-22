package ir.ramtung.tinyme.domain.exception;

public class InvalidStopLimitPriceException extends RuntimeException {

    public InvalidStopLimitPriceException(String msg) {
        super(msg);
    }

    public InvalidStopLimitPriceException() {
        super();
    }
}
