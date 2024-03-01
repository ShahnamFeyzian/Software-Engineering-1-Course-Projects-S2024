package self.SE1.CA1.controller;

import self.SE1.CA1.communication.Sender;
import self.SE1.CA1.communication.ResponseStatus;

public abstract class BaseController {
    
    private static Sender msgSender;

    public BaseController(Sender sender) {
        msgSender = sender;
    }

    protected static void response(ResponseStatus status, String msg) {
        msgSender.sendMessage(status, msg);
    }
}
