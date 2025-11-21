package es.ulpgc.searchengine.indexing;

import io.javalin.Javalin;
import es.ulpgc.searchengine.indexing.repository.DatamartSQLite;
import es.ulpgc.searchengine.indexing.messaging.IndexingEventConsumer;

public class IndexingApp {

    public static void main(String[] args) {

        int port = 7002;

        // Ruta DB (docker)
        String dbPath = System.getenv().getOrDefault("DATAMART_DB", "/app/datamart/index.db");
        DatamartSQLite repository = new DatamartSQLite(dbPath);
        repository.initSchema();

        Indexer indexer = new Indexer(repository);
        IndexingController controller = new IndexingController(indexer);

        System.out.println("[IndexingApp] Iniciando JMS consumer...");

        // CONSUMIDOR JMS — arranca en un hilo aparte
        IndexingEventConsumer consumer = new IndexingEventConsumer(indexer);
        Thread consumerThread = new Thread(consumer);
        consumerThread.setDaemon(true);
        consumerThread.start();

        // Servidor HTTP
        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(port);

        controller.register(app);

        System.out.println("Indexing Service running on port " + port);
    }
}
