package bank;

import javax.jms.Connection; 
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

public class Producer {

    public static void main(String[] args) throws JMSException {
        ActiveMQJMSConnectionFactory connectionFactory = new ActiveMQJMSConnectionFactory(
            "tcp://localhost:61616"
        );
        // javax.jms.Connection connection = connectionFactory.createConnection(); // Fix the type mismatch error
        // Session session = connection.createSession(
        //     false,
        //     Session.AUTO_ACKNOWLEDGE
        // );
        // Queue queue = session.createQueue("exampleQueue");

        // MessageProducer producer = session.createProducer(queue);

        // for (int i = 0; i < 10; i++) {
        //     TextMessage message = session.createTextMessage(
        //         "This is message " + i
        //     );
        //     producer.send(message);
        // }

        // producer.close();
        // session.close();
        // connection.close();
    }
}
