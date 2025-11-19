package es.ulpgc.searchengine.benchmarks;

import es.ulpgc.searchengine.benchmarks.utils.HttpHelper;
import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class BenchmarkRunner {
    private final String ingestionUrl;
    private final String indexingUrl;
    private final String searchUrl;
    private final Gson gson = new Gson();

    public BenchmarkRunner(String ingestionUrl, String indexingUrl, String searchUrl) {
        this.ingestionUrl = ingestionUrl;
        this.indexingUrl = indexingUrl;
        this.searchUrl = searchUrl;
    }

    public void runFullBenchmark() throws Exception {
        System.out.println(" Starting benchmark");
        List<Integer> books = List.of(46, 60, 61, 98, 1567, 1661, 2600, 5200);
        List<BenchmarkResult> results = new ArrayList<>();

        for (int bookId : books) {
            System.out.println("===============================");
            System.out.println(" Processing book " + bookId);
            results.add(runSingleBenchmark(bookId));
        }

        saveResults(results);
        System.out.println(" Benchmark finished. Results saved in /results/benchmarks.json and .csv");
    }

    private BenchmarkResult runSingleBenchmark(int bookId) {
        BenchmarkResult result = new BenchmarkResult(bookId);
        Instant startTotal = Instant.now();

        try {
            Instant startIngest = Instant.now();
            int ingestCode = HttpHelper.post(ingestionUrl + "/ingest/" + bookId);
            result.ingestionTime = Duration.between(startIngest, Instant.now()).toMillis();
            if (ingestCode != 200)
                throw new RuntimeException("Ingestion failed (HTTP " + ingestCode + ")");

            Instant startIndex = Instant.now();
            int indexCode = HttpHelper.post(indexingUrl + "/index/update/" + bookId);
            result.indexingTime = Duration.between(startIndex, Instant.now()).toMillis();
            if (indexCode != 200)
                throw new RuntimeException("Indexing failed (HTTP " + indexCode + ")");

            Instant startSearch = Instant.now();
            int basicCode = HttpHelper.get(searchUrl + "/search?q=the");
            int phraseCode = HttpHelper.get(searchUrl + "/search/phrase?phrase=the%20end");
            int boolCode = HttpHelper.get(searchUrl + "/search/advanced?q=the%20AND%20end");
            result.searchTime = Duration.between(startSearch, Instant.now()).toMillis();

            if (basicCode != 200 || phraseCode != 200 || boolCode != 200)
                throw new RuntimeException("Search failed (HTTP codes: basic=" + basicCode +
                        ", phrase=" + phraseCode + ", bool=" + boolCode + ")");

            result.success = true;
            System.out.println("Book " + bookId + " processed successfully!");

        } catch (Exception e) {
            result.success = false;
            System.err.println("Error processing book " + bookId + ": " + e.getMessage());
        }

        result.totalTime = Duration.between(startTotal, Instant.now()).toMillis();
        if (!result.success)
            System.err.println("Book " + bookId + " failed one or more phases.");

        return result;
    }

    private void saveResults(List<BenchmarkResult> results) throws Exception {
        Path dir = Paths.get("results");
        Files.createDirectories(dir);

        String json = gson.toJson(results);
        Files.writeString(dir.resolve("benchmarks.json"), json);

        Path csv = dir.resolve("benchmarks.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csv)) {
            writer.write("book_id,ingestion_ms,indexing_ms,search_ms,total_ms,success\n");
            for (BenchmarkResult r : results) {
                writer.write(String.format("%d,%d,%d,%d,%d,%b\n",
                        r.bookId, r.ingestionTime, r.indexingTime, r.searchTime, r.totalTime, r.success));
            }
        }
        System.out.println("Results written to results/benchmarks.csv");
    }
}
