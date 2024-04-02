package ir.ramtung.tinyme.domain.exception;

public class NotEnoughPositionException extends RuntimeException {

    public NotEnoughPositionException(String msg) {
        super(msg);
    }
    
    public NotEnoughPositionException() {
        super();
    }
}
