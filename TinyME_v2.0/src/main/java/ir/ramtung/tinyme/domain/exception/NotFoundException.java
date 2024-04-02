package ir.ramtung.tinyme.domain.exception;

public class NotFoundException extends RuntimeException{

    public NotFoundException(String msg) {
        super(msg);
    }

    public NotFoundException() {
        super();
    }
}
