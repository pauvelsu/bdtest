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
        app.post("/index/update/{book_id}", this::updateIndex);
        app.post("/index/rebuild", this::rebuildIndex);
        app.get("/index/status", this::status);
        app.get("/health", ctx -> ctx.status(200).result("OK"));
    }

    private void updateIndex(Context ctx) {
        try {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            boolean ok = indexer.indexBook(bookId);
            ctx.result(gson.toJson(Map.of(
                    "book_id", bookId,
                    "index", ok ? "updated" : "error"
            )));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of("error", e.getMessage())));
        }
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
