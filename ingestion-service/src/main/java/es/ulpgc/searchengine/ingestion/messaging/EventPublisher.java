package es.ulpgc.searchengine.ingestion.messaging;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class EventPublisher {

    private final String brokerUrl;
    private final String queueName;

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public EventPublisher(String brokerUrl, String queueName) {
        this.brokerUrl = brokerUrl;
        this.queueName = queueName;
        init();
    }

    /** Inicializa conexión JMS con reintentos */
    private void init() {
        int retries = 0;

        while (retries < 20) {
            try {
                System.out.println("[EventPublisher] Conectando a " + brokerUrl);

                ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

                this.connection = factory.createConnection();
                this.connection.start();

                this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination queue = session.createQueue(queueName);

                this.producer = session.createProducer(queue);

                System.out.println("[EventPublisher] JMS inicializado correctamente.");
                return;

            } catch (Exception e) {
                retries++;
                System.err.println("[EventPublisher] ERROR al iniciar JMS. Reintentando... (" + retries + "/20)");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        System.err.println("[EventPublisher] NO SE PUDO CONECTAR AL BROKER.");
    }

    /** Publica un evento document.ingested con el ID */
    public void publishBookIngested(int bookId) {
        try {
            if (producer == null) {
                System.err.println("[EventPublisher] JMS NO inicializado. Mensaje ignorado: " + bookId);
                return;
            }

            TextMessage msg = session.createTextMessage(String.valueOf(bookId));
            producer.send(msg);

            System.out.println("[EventPublisher] Evento enviado: bookId=" + bookId);

        } catch (Exception e) {
            System.err.println("[EventPublisher] Error enviando mensaje: " + e.getMessage());
        }
    }
}
