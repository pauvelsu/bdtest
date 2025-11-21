package es.ulpgc.searchengine.indexing.messaging;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import es.ulpgc.searchengine.indexing.Indexer;

public class IndexingEventConsumer implements Runnable {

    private final Indexer indexer;
    private static final String BROKER_URL = "tcp://activemq:61616";
    private static final String QUEUE_NAME = "document.ingested";

    public IndexingEventConsumer(Indexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public void run() {
        int retries = 0;

        while (retries < 20) {
            try {
                System.out.println("[IndexingConsumer] Intentando conectar a ActiveMQ...");

                ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
                Connection connection = factory.createConnection();
                connection.start();

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination queue = session.createQueue(QUEUE_NAME);

                MessageConsumer consumer = session.createConsumer(queue);

                consumer.setMessageListener(msg -> {
                    try {
                        int id = Integer.parseInt(((TextMessage) msg).getText());
                        System.out.println("[IndexingConsumer] Recibido ID = " + id);
                        indexer.indexDocument(id);
                    } catch (Exception e) {
                        System.err.println("[IndexingConsumer] Error procesando mensaje: " + e.getMessage());
                    }
                });

                System.out.println("[IndexingConsumer] Conectado y escuchando mensajes.");
                return;

            } catch (Exception e) {
                retries++;
                System.out.println("[IndexingConsumer] Fallo, reintentando en 2s... (" + retries + "/20)");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        System.err.println("[IndexingConsumer] ERROR: No se pudo conectar al broker.");
    }
}