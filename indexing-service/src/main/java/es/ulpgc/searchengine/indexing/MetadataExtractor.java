package es.ulpgc.searchengine.indexing;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;
import java.util.*;

public class MetadataExtractor {
    public static class Meta {
        public String title;
        public String author;
        public String language;
        public int year;

        public Meta(String t, String a, String l, int y) {
            title = t != null && !t.trim().isEmpty() ? t.trim() : "Unknown";
            author = a != null && !a.trim().isEmpty() ? a.trim() : "Unknown";
            language = l != null && !l.trim().isEmpty() ? l.trim() : "en";
            year = y;
        }
    }

    public static Meta extract(Path headerFile) throws IOException {
        List<String> lines = Files.readAllLines(headerFile);
        String fullText = String.join("\n", lines);
        System.out.println("Analyzing header for metadata extraction...");
        System.out.println("First 10 lines:");
        for (int i = 0; i < Math.min(10, lines.size()); i++) {
            System.out.println("  " + i + ": " + lines.get(i));
        }

        String title = extractTitle(fullText, lines);
        String author = extractAuthor(fullText, lines);
        String language = extractLanguage(fullText);
        int year = extractYear(fullText);
        System.out.println("Extracted metadata:");
        System.out.println("  Title: " + title);
        System.out.println("  Author: " + author);
        System.out.println("  Language: " + language);
        System.out.println("  Year: " + year);

        return new Meta(title, author, language, year);
    }

    private static String extractTitle(String fullText, List<String> lines) {
        String[][] patterns = {
                {"Title: (.+)", "1"},
                {"Title\\s*: (.+)", "1"},
                {"The Project Gutenberg eBook of (.+)", "1"},
                {"\"(.+)\"", "1"},
                {"^(.+?)\\n", "1"}
        };

        for (String[] pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern[0], Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(fullText);
                if (m.find()) {
                    String title = m.group(Integer.parseInt(pattern[1])).trim();
                    if (isValidTitle(title)) {
                        return cleanText(title);
                    }
                }
            } catch (Exception e) {
            }
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() &&
                    !trimmed.startsWith("Author:") &&
                    !trimmed.startsWith("Language:") &&
                    !trimmed.startsWith("***") &&
                    trimmed.length() > 5 &&
                    trimmed.length() < 200) {
                return cleanText(trimmed);
            }
        }
        return "Unknown";
    }

    private static String extractAuthor(String fullText, List<String> lines) {
        String[][] patterns = {
                {"Author: (.+)", "1"},
                {"Author\\s*: (.+)", "1"},
                {"by (.+?)\\n", "1"},
                {"Written by (.+)", "1"},
                {"by (.+?)$", "1"}
        };

        for (String[] pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern[0], Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(fullText);
                if (m.find()) {
                    String author = m.group(Integer.parseInt(pattern[1])).trim();
                    if (isValidAuthor(author)) {
                        return cleanText(author);
                    }
                }
            } catch (Exception e) {
            }
        }

        return "Unknown";
    }

    private static String extractLanguage(String fullText) {
        String[] patterns = {
                "Language: (.+)",
                "Language\\s*: (.+)",
                "Language:.*?([a-zA-Z]{2,})"
        };

        for (String pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(fullText);
                if (m.find()) {
                    String lang = m.group(1).trim();
                    if (!lang.isEmpty() && lang.length() <= 20) {
                        return cleanText(lang).toLowerCase();
                    }
                }
            } catch (Exception e) {
            }
        }
        return "en";
    }

    private static int extractYear(String fullText) {
        Matcher m = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b").matcher(fullText);
        List<Integer> years = new ArrayList<>();
        while (m.find()) {
            try {
                int year = Integer.parseInt(m.group(1));
                if (year >= 1000 && year <= 2024) {
                    years.add(year);
                }
            } catch (Exception ignored) {}
        }

        if (!years.isEmpty()) {
            return years.get(0);
        }
        return -1;
    }

    private static boolean isValidTitle(String title) {
        if (title == null || title.trim().isEmpty()) return false;
        String lower = title.toLowerCase();
        return !lower.contains("gutenberg") &&
                !lower.contains("project") &&
                !lower.contains("ebook") &&
                title.length() > 2 &&
                title.length() < 200;
    }

    private static boolean isValidAuthor(String author) {
        if (author == null || author.trim().isEmpty()) return false;
        String lower = author.toLowerCase();
        return !lower.contains("gutenberg") &&
                !lower.contains("project") &&
                !lower.contains("unknown") &&
                author.length() > 2 &&
                author.length() < 100;
    }

    private static String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}