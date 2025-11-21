package es.ulpgc.searchengine.indexing;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.Map;

public class IndexingController {

    private static final Gson gson = new Gson();
    private final Indexer indexer;

    public IndexingController(Indexer indexer) {
        this.indexer = indexer;
    }

    public void register(Javalin app) {
        app.post("/index/rebuild", this::rebuildIndex);
        app.get("/index/status", this::status);
        app.get("/health", ctx -> ctx.status(200).result("OK"));
    }

    private void rebuildIndex(Context ctx) {
        try {
            int count = indexer.rebuildAll();
            ctx.result(gson.toJson(Map.of(
                    "books_processed", count,
                    "status", "rebuild_complete"
            )));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of("error", e.getMessage())));
        }
    }

    private void status(Context ctx) {
        try {
            Map<String, Object> info = indexer.getStatus();
            ctx.result(gson.toJson(info));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of("error", e.getMessage())));
        }
    }
}