package ir.ramtung.tinyme.domain.exception;

public class UpdateMinimumExecutionQuantityException extends RuntimeException {

    public UpdateMinimumExecutionQuantityException(String msg) {
        super(msg);
    }

    public UpdateMinimumExecutionQuantityException() {
        super();
    }
}
