package es.ulpgc.searchengine.search;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.*;

public class SearchController {
    private static final Gson gson = new Gson();
    private final AdvancedSearchEngine engine;

    public SearchController(AdvancedSearchEngine engine) {
        this.engine = engine;
    }

    public void register(Javalin app) {
        app.get("/search", this::search);
        app.get("/search/advanced", this::advancedSearch);
        app.get("/search/phrase", this::phraseSearch);
        app.get("/search/range", this::rangeSearch);
        app.get("/search/stats", this::searchStats);
        app.post("/search/refresh", this::refreshCache);
        app.get("/health", ctx -> ctx.status(200).result("OK"));
        app.get("/status", ctx -> ctx.result(gson.toJson(Map.of("service", "search", "status", "running"))));

        System.out.println("Search endpoints registered:");
        System.out.println("- GET  /search");
        System.out.println("- GET  /search/advanced");
        System.out.println("- GET  /search/phrase");
        System.out.println("- GET  /search/range");
        System.out.println("- GET  /search/stats");
        System.out.println("- POST /search/refresh");
    }

    private void search(Context ctx) {
        try {
            String term = ctx.queryParam("q");
            if (term == null || term.isBlank()) {
                ctx.status(400).result(gson.toJson(createErrorResponse("missing_query_param")));
                return;
            }
            String author = ctx.queryParam("author");
            String language = ctx.queryParam("language");
            String yearStr = ctx.queryParam("year");
            Integer year = (yearStr != null && !yearStr.isBlank()) ? Integer.parseInt(yearStr) : null;
            List<Map<String, Object>> results = engine.search(term, author, language, year);
            Map<String, Object> response = new HashMap<>();
            response.put("query", term);
            Map<String, Object> filters = new HashMap<>();
            filters.put("author", author != null ? author : "");
            filters.put("language", language != null ? language : "");
            filters.put("year", year != null ? year : "");
            response.put("filters", filters);
            response.put("count", results.size());
            response.put("results", results != null ? results : new ArrayList<>());
            ctx.result(gson.toJson(response));

        } catch (Exception e) {
            System.err.println("Error in search: " + e.getMessage());
            ctx.status(500).result(gson.toJson(createErrorResponse("internal_server_error")));
        }
    }

    private void advancedSearch(Context ctx) {
        System.out.println("Received advanced search request");
        String query = ctx.queryParam("q");
        System.out.println("   - Query: " + query);

        if (query == null || query.isBlank()) {
            ctx.status(400).result(gson.toJson(createErrorResponse("missing_query_param")));
            return;
        }

        String author = ctx.queryParam("author");
        String language = ctx.queryParam("language");
        String yearStr = ctx.queryParam("year");
        Integer year = (yearStr != null && !yearStr.isBlank()) ? Integer.parseInt(yearStr) : null;

        try {
            List<Map<String, Object>> results = engine.booleanSearch(query, author, language, year);
            System.out.println("Advanced search found " + results.size() + " results");
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("type", "boolean_search");
            Map<String, Object> filters = new HashMap<>();
            filters.put("author", author != null ? author : "");
            filters.put("language", language != null ? language : "");
            filters.put("year", year != null ? year : "");
            response.put("filters", filters);
            response.put("count", results.size());
            response.put("results", results);
            ctx.result(gson.toJson(response));

        } catch (Exception e) {
            System.err.println("Error in advanced search: " + e.getMessage());
            ctx.status(500).result(gson.toJson(Map.of(
                    "error", "search_failed",
                    "message", e.getMessage()
            )));
        }
    }

    private void phraseSearch(Context ctx) {
        String phrase = ctx.queryParam("phrase");
        if (phrase == null || phrase.isBlank()) {
            ctx.status(400).result(gson.toJson(Map.of("error", "missing_phrase_param")));
            return;
        }
        String author = ctx.queryParam("author");
        String language = ctx.queryParam("language");
        String yearStr = ctx.queryParam("year");
        Integer year = (yearStr != null && !yearStr.isBlank()) ? Integer.parseInt(yearStr) : null;

        try {
            List<Map<String, Object>> results = engine.searchPhrase(phrase, author, language, year);
            Map<String, Object> response = new HashMap<>();
            response.put("phrase", phrase);
            response.put("type", "phrase_search");
            Map<String, Object> filters = new HashMap<>();
            filters.put("author", author != null ? author : "");
            filters.put("language", language != null ? language : "");
            filters.put("year", year != null ? year : "");
            response.put("filters", filters);
            response.put("count", results.size());
            response.put("results", results);
            ctx.result(gson.toJson(response));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of(
                    "error", "search_failed",
                    "message", e.getMessage()
            )));
        }
    }

    private void rangeSearch(Context ctx) {
        String startYearStr = ctx.queryParam("start_year");
        String endYearStr = ctx.queryParam("end_year");
        String term = ctx.queryParam("q");
        if (startYearStr == null || endYearStr == null) {
            ctx.status(400).result(gson.toJson(Map.of("error", "missing_year_range")));
            return;
        }

        try {
            Integer startYear = Integer.parseInt(startYearStr);
            Integer endYear = Integer.parseInt(endYearStr);
            List<Map<String, Object>> results = engine.searchByYearRange(startYear, endYear, term);
            ctx.result(gson.toJson(Map.of(
                    "range", startYear + "-" + endYear,
                    "term", term,
                    "type", "year_range_search",
                    "count", results.size(),
                    "results", results
            )));
        } catch (NumberFormatException e) {
            ctx.status(400).result(gson.toJson(Map.of("error", "invalid_year_format")));
        }
    }

    private void searchStats(Context ctx) {
        Map<String, Object> stats = engine.getSearchStats();
        ctx.result(gson.toJson(stats));
    }

    private void refreshCache(Context ctx) {
        engine.refreshCache();
        ctx.result(gson.toJson(Map.of("status", "cache_refreshed")));
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        return errorResponse;
    }
}
