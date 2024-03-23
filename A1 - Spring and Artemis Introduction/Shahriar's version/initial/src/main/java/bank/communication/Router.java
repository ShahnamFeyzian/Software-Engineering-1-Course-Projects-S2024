package bank.communication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import bank.Bank;

@Service
public class Router {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Bank bank;

	@Value("${OUTQ}")
	private String OUTQ;

	@SuppressWarnings("null")
	public void send(String message) {
		System.out.println("Sending message: " + message);
		jmsTemplate.convertAndSend(OUTQ, message);
	}

	@JmsListener(destination = "${INQ}")
	public void listener(String message) {
		System.out.println("Received message: " + message);
		bank.callback(message);
	}
}
