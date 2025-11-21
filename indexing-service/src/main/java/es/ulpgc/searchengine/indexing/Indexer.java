package es.ulpgc.searchengine.indexing;

import es.ulpgc.searchengine.indexing.repository.DatamartSQLite;
import es.ulpgc.searchengine.indexing.MetadataExtractor.Meta;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Indexer {

    private final DatamartSQLite repository;
    private static final String DATALAKE_PATH = "./datalake";

    public Indexer(DatamartSQLite repository) {
        this.repository = repository;
    }

    /** NUEVO: método thread-safe llamado desde el consumidor JMS */
    public synchronized boolean indexDocument(int bookId) {
        return indexBook(bookId);
    }

    public boolean indexBook(int bookId) {
        try {
            Path bookDir = findBookDirectory(bookId);
            if (bookDir == null) throw new IllegalStateException("Book " + bookId + " not found in datalake.");

            Path header = bookDir.resolve("header.txt");
            Path body = bookDir.resolve("body.txt");

            if (!Files.exists(header) || !Files.exists(body))
                throw new IllegalStateException("Missing header/body for " + bookId);

            Meta meta = MetadataExtractor.extract(header);
            String content = Files.readString(body);

            if (content.isBlank())
                throw new IllegalStateException("Empty content for book " + bookId);

            Map<String, List<Integer>> index = buildInvertedIndex(content);

            repository.deleteIndexForBook(bookId);
            repository.insertOrUpdateBook(bookId, meta.title, meta.author, meta.language, meta.year, content);
            repository.insertIndex(bookId, index);

            System.out.printf("Indexed %d (%d terms): %s by %s%n",
                    bookId, index.size(), meta.title, meta.author);

            return true;

        } catch (Exception e) {
            logError("Error indexing book " + bookId, e);
            return false;
        }
    }

    public int rebuildAll() {
        try {
            List<Path> bookDirs = findAllBookDirectories();
            AtomicInteger success = new AtomicInteger();
            int total = bookDirs.size();

            System.out.println("Starting index rebuild for " + total + " books...");
            for (Path dir : bookDirs) {
                int bookId = safeParseInt(dir.getFileName().toString());
                if (bookId > 0 && indexBook(bookId)) success.incrementAndGet();
            }
            System.out.printf("Rebuild completed: %d/%d books.%n", success.get(), total);
            return success.get();

        } catch (Exception e) {
            logError("Error rebuilding index", e);
            return 0;
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("indexing_stats", repository.getStats());
        status.put("datalake_stats", getDatalakeStats());
        status.put("last_operation", new Date().toString());
        return status;
    }

    private Map<String, Object> getDatalakeStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Path> bookDirs = findAllBookDirectories();
            Set<String> dates = new HashSet<>();
            Set<String> timestamps = new HashSet<>();

            for (Path dir : bookDirs) {
                Path minute = dir.getParent();
                Path hour = minute != null ? minute.getParent() : null;
                Path date = hour != null ? hour.getParent() : null;

                if (date != null)
                    timestamps.add(date.getFileName() + "/" + hour.getFileName() + "/" + minute.getFileName());
                if (date != null)
                    dates.add(date.getFileName().toString());
            }

            stats.put("total_books", bookDirs.size());
            stats.put("unique_dates", dates.size());
            stats.put("unique_timestamps", timestamps.size());
            stats.put("structure", "YYYYMMDD/HH/mm/bookId/");
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private Map<String, List<Integer>> buildInvertedIndex(String text) {
        Map<String, List<Integer>> index = new HashMap<>();
        String[] tokens = text.toLowerCase()
                .replaceAll("[^a-záéíóúüñ\\s]", " ")
                .split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].trim();
            if (word.length() > 2)
                index.computeIfAbsent(word, k -> new ArrayList<>()).add(i);
        }
        return index;
    }

    private Path findBookDirectory(int bookId) throws Exception {
        try (Stream<Path> stream = Files.walk(Paths.get(DATALAKE_PATH))) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals(String.valueOf(bookId)))
                    .findFirst().orElse(null);
        }
    }

    private List<Path> findAllBookDirectories() throws Exception {
        try (Stream<Path> stream = Files.walk(Paths.get(DATALAKE_PATH))) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.matches("\\d+") &&      // bookId
                                !name.matches("\\d{8}") &&   // fecha
                                !name.matches("\\d{2}");     // hour/minute
                    })
                    .toList();
        }
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return -1; }
    }

    private void logError(String msg, Exception e) {
        System.err.println(msg + ": " + e.getMessage());
    }
}