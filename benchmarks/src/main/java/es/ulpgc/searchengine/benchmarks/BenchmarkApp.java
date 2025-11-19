package es.ulpgc.searchengine.benchmarks;

public class BenchmarkApp {
    public static void main(String[] args) {
        String ingestionUrl = System.getenv().getOrDefault("INGESTION_URL", "http://localhost:7001");
        String indexingUrl  = System.getenv().getOrDefault("INDEXING_URL",  "http://localhost:7002");
        String searchUrl    = System.getenv().getOrDefault("SEARCH_URL",    "http://localhost:7003");


        BenchmarkRunner runner = new BenchmarkRunner(ingestionUrl, indexingUrl, searchUrl);

        try {
            runner.runFullBenchmark();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error running benchmarks");
        }
    }
}
