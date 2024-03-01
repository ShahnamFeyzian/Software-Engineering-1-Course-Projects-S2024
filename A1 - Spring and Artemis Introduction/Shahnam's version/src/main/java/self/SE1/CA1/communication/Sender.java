package self.SE1.CA1.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class Sender {

    private final JmsTemplate jmsTemplate;
    
    @Value("${jms.output.name}")
    private String jmsOutputName;

    public Sender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendMessage(ResponseStatus status, String msg) {
        String finalMsg = ResponseStatus.getStatusCode(status) + " " + msg;
        jmsTemplate.convertAndSend(jmsOutputName, finalMsg);
        System.out.println(finalMsg);
    }
}
