package es.ulpgc.searchengine.ingestion;

import io.javalin.Javalin;

public class IngestionApp {
    public static void main(String[] args) {
        int port = 7001;

        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(port);
        IngestionController controller = new IngestionController();
        controller.register(app);
        System.out.println("Ingestion Service running on port " + port);
    }
}
