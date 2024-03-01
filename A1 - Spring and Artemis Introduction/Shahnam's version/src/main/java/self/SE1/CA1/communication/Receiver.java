package self.SE1.CA1.communication;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {
    
    private final Router router;

    public Receiver(Router router) {
        this.router = router;
    }

    @JmsListener(destination = "${jms.input.name}")
    public void receiveMessage(String msg) {
        String[] splitedMsg = msg.split("\\s+");
        router.callProperController(splitedMsg);
    }
}
