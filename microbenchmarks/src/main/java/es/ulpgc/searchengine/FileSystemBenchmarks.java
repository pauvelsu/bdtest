package es.ulpgc.searchengine;

import org.openjdk.jmh.annotations.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileSystemBenchmarks {

    private Path basePath;
    @Setup(Level.Iteration)
    public void setup() {
        try {
            basePath = Paths.get("./datalake");
            Files.createDirectories(basePath.resolve("20240115/14/30/12345"));
        } catch (IOException e) {
            logError("Error in setup", e);
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> safeDelete(path));
        } catch (IOException e) {
            logError("Error cleaning datalake in teardown", e);
        }
    }

    @Benchmark
    public boolean findBookInNewStructure() {
        try (Stream<Path> paths = Files.walk(basePath)) {
            return paths.filter(Files::isDirectory)
                    .anyMatch(p -> p.getFileName().toString().equals("12345"));
        } catch (IOException e) {
            logError("Error in findBookInNewStructure", e);
            return false;
        }
    }

    @Benchmark
    public Path createTemporalStructure() {
        try {
            Path bookDir = getTimestampedPath("99999");
            Files.createDirectories(bookDir);
            return bookDir;
        } catch (IOException e) {
            logError("Error creating temporal structure", e);
            return null;
        }
    }

    @Benchmark
    public int countBooksInStructure() {
        try (Stream<Path> paths = Files.walk(basePath)) {
            return (int) paths.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().matches("\\d+"))
                    .count();
        } catch (IOException e) {
            logError("Error counting books in structure", e);
            return 0;
        }
    }

    @Benchmark
    public boolean checkBookExistsByTimestamp() {
        try {
            Path specificPath = basePath.resolve("20240115/14/30/12345");
            return Files.exists(specificPath);
        } catch (Exception e) {
            logError("Error checking book existence by timestamp", e);
            return false;
        }
    }

    @Benchmark
    public Path createComplexStructure() {
        try {
            Path baseDir = createTimeStructure();
            for (int i = 1; i <= 5; i++) {
                Path bookDir = baseDir.resolve(String.valueOf(10000 + i));
                Files.createDirectories(bookDir);
                Files.writeString(bookDir.resolve("header.txt"),
                        "Title: Test Book " + i + "\nAuthor: Test Author\nLanguage: en\nYear: 2024");
                Files.writeString(bookDir.resolve("body.txt"),
                        "This is sample content for book " + i + ".\nIt contains some text for testing.");
            }
            return baseDir;
        } catch (IOException e) {
            logError("Error creating complex structure", e);
            return null;
        }
    }

    @Benchmark
    public int walkTemporalStructure() {
        try (Stream<Path> paths = Files.walk(basePath)) {
            return (int) paths.filter(Files::isDirectory)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.matches("\\d{8}") ||
                                name.matches("\\d{2}") ||
                                name.matches("\\d+");
                    })
                    .count();
        } catch (IOException e) {
            logError("Error walking temporal structure", e);
            return 0;
        }
    }

    @Benchmark
    public boolean findBookByWalking() {
        try (Stream<Path> paths = Files.walk(basePath)) {
            return paths.filter(Files::isDirectory)
                    .anyMatch(this::isBookUnderValidTimestamp);
        } catch (IOException e) {
            logError("Error finding book by walking", e);
            return false;
        }
    }

    private Path getTimestampedPath(String bookId) {
        LocalDateTime now = LocalDateTime.now();
        return basePath.resolve(
                now.toLocalDate().toString().replace("-", "") + "/" +
                        String.format("%02d", now.getHour()) + "/" +
                        String.format("%02d", now.getMinute()) + "/" +
                        bookId
        );
    }

    private Path createTimeStructure() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        Path dir = basePath.resolve(
                now.toLocalDate().toString().replace("-", "") + "/" +
                        String.format("%02d", now.getHour()) + "/" +
                        String.format("%02d", now.getMinute())
        );
        Files.createDirectories(dir);
        return dir;
    }

    private boolean isBookUnderValidTimestamp(Path path) {
        String name = path.getFileName().toString();
        if (!name.equals("12345")) return false;
        Path minute = path.getParent();
        Path hour = minute != null ? minute.getParent() : null;
        Path date = hour != null ? hour.getParent() : null;
        return date != null &&
                minute.getFileName().toString().matches("\\d{2}") &&
                hour.getFileName().toString().matches("\\d{2}") &&
                date.getFileName().toString().matches("\\d{8}");
    }

    private void safeDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logError("Error deleting " + path, e);
        }
    }

    private void logError(String msg, Exception e) {
        System.err.println(msg + ": " + e.getMessage());
    }
}
