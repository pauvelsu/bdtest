package es.ulpgc.searchengine.ingestion;

import io.javalin.Javalin;
import es.ulpgc.searchengine.ingestion.messaging.EventPublisher;

public class IngestionApp {
    public static void main(String[] args) {
        int port = 7001;

        // URL del broker (dentro de Docker es "activemq:61616")
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("INGESTION_QUEUE", "document.ingested");

        EventPublisher publisher = new EventPublisher(brokerUrl, queueName);

        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(port);

        IngestionController controller = new IngestionController(publisher);
        controller.register(app);

        System.out.println("Ingestion Service running on port " + port);
    }
}
