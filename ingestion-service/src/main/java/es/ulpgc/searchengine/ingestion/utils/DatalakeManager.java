package es.ulpgc.searchengine.ingestion.utils;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DatalakeManager {

    private static final Path BASE = Paths.get("./datalake");

    public static Path save(int id, String rawText) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Path dir = BASE.resolve(
                now.toLocalDate().toString().replace("-", "") + "/" +
                        String.format("%02d", now.getHour()) + "/" +
                        String.format("%02d", now.getMinute()) + "/" +
                        id);

        Files.createDirectories(dir);
        Files.writeString(dir.resolve("raw.txt"), rawText);

        return dir;
    }

    public static boolean exists(int id) {
        try {
            return Files.walk(BASE)
                    .filter(Files::isDirectory)
                    .anyMatch(p -> p.getFileName().toString().equals(String.valueOf(id)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * -----------------------------------------
     *   List all book IDs in the datalake
     * -----------------------------------------
     */
    public static List<Integer> list() {
        List<Integer> ids = new ArrayList<>();

        if (!Files.exists(BASE)) {
            return ids;
        }

        try (Stream<Path> walk = Files.walk(BASE)) {
            walk.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.matches("\\d+"))   // Solo carpetas numéricas = bookId
                    .map(Integer::parseInt)
                    .distinct()
                    .sorted()
                    .forEach(ids::add);
        } catch (Exception e) {
            System.err.println("Error listing books in datalake: " + e.getMessage());
        }

        return ids;
    }
}
