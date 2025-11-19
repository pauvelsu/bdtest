package es.ulpgc.searchengine.ingestion;

import es.ulpgc.searchengine.ingestion.utils.DatalakeManager;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import es.ulpgc.searchengine.ingestion.utils.Downloader;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class IngestionController {
    private static final Gson gson = new Gson();

    public void register(Javalin app) {
        app.post("/ingest/{book_id}", this::downloadBook);
        app.get("/ingest/status/{book_id}", this::checkStatus);
        app.get("/ingest/list", this::listBooks);
        app.get("/health", ctx -> ctx.status(200).result("OK"));
    }

    private void downloadBook(Context ctx) {
        try {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            String rawText = Downloader.get("https://www.gutenberg.org/files/" + bookId + "/" + bookId + "-0.txt");
            Path bookDir = DatalakeManager.save(bookId, rawText);
            List<String> lines = Arrays.asList(rawText.split("\n"));
            int start = -1, end = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).toLowerCase();
                if (start == -1 && line.contains("*** start of")) start = i + 1;
                if (line.contains("*** end of")) {
                    end = i;
                    break;
                }
            }

            List<String> header = lines.subList(0, start > 0 ? start : Math.min(50, lines.size()));
            List<String> body = (start >= 0 && end > start)
                    ? lines.subList(start, end)
                    : lines.subList(Math.min(50, lines.size()), lines.size());

            Files.write(bookDir.resolve("header.txt"), header);
            Files.write(bookDir.resolve("body.txt"), body);
            System.out.println("Book " + bookId + ": header=" + header.size() + " lines, body=" + body.size() + " lines");
            ctx.status(200).result(gson.toJson(Map.of(
                    "book_id", bookId,
                    "status", "downloaded",
                    "path", bookDir.toString()
            )));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of(
                    "error", "download_failed",
                    "message", e.getMessage()
            )));
        }
    }

    private void checkStatus(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        boolean exists = DatalakeManager.exists(bookId);
        ctx.status(200).result(gson.toJson(Map.of(
                "book_id", bookId,
                "status", exists ? "available" : "missing"
        )));
    }

    private void listBooks(Context ctx) {
        ctx.result(gson.toJson(DatalakeManager.list()));
    }
}
