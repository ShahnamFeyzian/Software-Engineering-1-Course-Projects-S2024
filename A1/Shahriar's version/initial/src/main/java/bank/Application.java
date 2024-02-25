package bank;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.core.JmsTemplate;

@SpringBootApplication
public class Application {

    private static final String INQ = "INQ";
	private static final String OUTQ = "OUTQ";

	@Autowired
	private JmsTemplate jmsTemplate;

	// public static void main(String[] args) {
	// 	SpringApplication.run(Application.class, args);
	// }

	@JmsListener(destination = INQ)
	public void listener(String message) {
		System.out.println("Message received: " + message);
		send("Replying to " + message + "...");
	}

	@SuppressWarnings("null")
	public void send(String message) {
		System.out.println("Sending message: " + message);
		jmsTemplate.convertAndSend(OUTQ, message);
	}
}

