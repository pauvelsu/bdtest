package es.ulpgc.searchengine.control;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;

import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;

public class Controller {
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, String> services = Map.of(
            "ingestion", "http://ingestion-service:7001",
            "indexing", "http://indexing-service:7002",
            "search", "http://search-service:7003"
    );

    public void register(Javalin app) {
        try {
            app.get("/control/status", this::getStatus);
            app.post("/control/run", this::runPipeline);
            app.get("/control/run", this::runPipeline);
        } catch (Exception e) {
            System.err.println("Error registering endpoints: " + e.getMessage());
        }
    }

    private void getStatus(Context ctx) {
        try {
            Map<String, Object> status = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : services.entrySet()) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(entry.getValue() + "/status"))
                        .GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                status.put(entry.getKey(), "OK (" + res.statusCode() + ")");
            }
            ctx.result(gson.toJson(status));
        } catch (Exception e) {
            System.err.println("Error retrieving service status: " + e.getMessage());
            ctx.status(500).result("{\"error\":\"Failed to retrieve status\"}");
        }
    }

    private void runPipeline(Context ctx) {
        try {
            List<Integer> books = List.of(11, 84, 98, 1342, 1661);
            List<Map<String, Object>> report = new ArrayList<>();

            for (int id : books) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("book_id", id);

                post(services.get("ingestion") + "/ingest/" + id);
                String statusJson = get(services.get("ingestion") + "/ingest/status/" + id);

                if (!statusJson.contains("\"status\":\"available\"")) {
                    entry.put("status", "ingestion_failed");
                    report.add(entry);
                    continue;
                }

                post(services.get("indexing") + "/index/update/" + id);
                entry.put("status", "indexed");
                report.add(entry);
            }

            post(services.get("search") + "/search/refresh");
            testAdvancedSearch();

            ctx.result(gson.toJson(Map.of(
                    "status", "Pipeline executed with advanced search",
                    "books_processed", report.size(),
                    "details", report
            )));
        } catch (Exception e) {
            System.err.println("Error running pipeline: " + e.getMessage());
            ctx.status(500).result("{\"error\":\"Pipeline execution failed\"}");
        }
    }

    private void testAdvancedSearch() {
        try {
            String[] testQueries = {
                    "/search?q=love",
                    "/search/phrase?phrase=art of war",
                    "/search/advanced?q=java AND programming",
                    "/search/range?start_year=1800&end_year=1900&q=adventure"
            };

            for (String query : testQueries) {
                String response = get(services.get("search") + query);
                System.out.println("Test query: " + query + " → " +
                        response.substring(0, Math.min(100, response.length())));
            }
        } catch (Exception e) {
            System.err.println("Error testing advanced search: " + e.getMessage());
        }
    }

    private void post(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300)
            throw new RuntimeException("HTTP " + res.statusCode() + " → " + res.body());
    }

    private String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return res.body();
    }

    public static void main(String[] args) {
        try {
            Javalin app = Javalin.create().start(8080);
            Controller controller = new Controller();
            controller.register(app);
        } catch (Exception e) {
            System.err.println("Fatal error starting controller: " + e.getMessage());
        }
    }
}
