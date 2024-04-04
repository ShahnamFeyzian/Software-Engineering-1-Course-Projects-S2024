package ir.ramtung.tinyme.domain.exception;

public class CantQueueOrderException extends RuntimeException {

    public CantQueueOrderException(String msg) {
        super(msg);
    }
    
    public CantQueueOrderException() {
        super();
    }
}
