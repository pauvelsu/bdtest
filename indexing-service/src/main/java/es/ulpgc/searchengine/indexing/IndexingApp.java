package es.ulpgc.searchengine.indexing;

import io.javalin.Javalin;
import es.ulpgc.searchengine.indexing.repository.DatamartSQLite;

public class IndexingApp {
    public static void main(String[] args) {
        int port = 7002;
        String dbPath = System.getenv().getOrDefault("DATAMART_DB", "./datamart/index.db");

        DatamartSQLite repository = new DatamartSQLite(dbPath);
        repository.initSchema();

        Indexer indexer = new Indexer(repository);
        IndexingController controller = new IndexingController(indexer);

        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json").start(port);
        controller.register(app);

        System.out.println("Indexing Service running on port " + port);
    }
}
