package ir.ramtung.tinyme.domain.exception;

public class CantRollbackTradeException extends RuntimeException {
    
    public CantRollbackTradeException(String msg) {
        super(msg);
    }

    public CantRollbackTradeException() {
        super();
    }
}
