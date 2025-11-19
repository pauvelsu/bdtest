package es.ulpgc.searchengine.search;

import io.javalin.Javalin;
import es.ulpgc.searchengine.search.repository.DatamartSQLite;

public class SearchApp {
    public static void main(String[] args) {
        int port = 7003;
        String dbPath = System.getenv().getOrDefault("DATAMART_DB", "./datamart/index.db");
        DatamartSQLite repository = new DatamartSQLite(dbPath);
        repository.initSchema();
        AdvancedSearchEngine engine = new AdvancedSearchEngine(repository);
        SearchController controller = new SearchController(engine);

        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json").start(port);
        controller.register(app);

        System.out.println("Advanced Search Service running on port " + port);
        System.out.println("Available endpoints:");
        System.out.println("GET  /search?q=term&author=name&language=lang&year=yyyy");
        System.out.println("POST /search/advanced?q=query (supports AND/OR/NOT)");
        System.out.println("GET  /search/phrase?phrase=exact phrase");
        System.out.println("GET  /search/range?start_year=yyyy&end_year=yyyy&q=term");
        System.out.println("GET  /search/stats");
    }
}