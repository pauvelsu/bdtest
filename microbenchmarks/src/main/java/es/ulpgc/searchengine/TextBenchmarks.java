package es.ulpgc.searchengine;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.util.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class TextBenchmarks {

    private String sampleText;

    @Setup(Level.Iteration)
    public void setup() {
        sampleText = "Title: Pride and Prejudice\nAuthor: Jane Austen\nLanguage: en\nYear: 1813\n\n"
                + "It is a truth universally acknowledged, that a single man in possession of a good fortune...";
    }

    @Benchmark
    public Map<String, List<Integer>> buildInvertedIndex() {
        Map<String, List<Integer>> index = new HashMap<>();
        String[] tokens = sampleText.toLowerCase()
                .replaceAll("[^a-záéíóúüñ\\s]", "")
                .split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].trim();
            if (!word.isEmpty()) {
                index.computeIfAbsent(word, k -> new ArrayList<>()).add(i);
            }
        }
        return index;
    }

    @Benchmark
    public String extractTitle() {
        Matcher m = Pattern.compile("Title: (.*)", Pattern.MULTILINE).matcher(sampleText);
        return m.find() ? m.group(1).trim() : "Unknown";
    }

    @Benchmark
    public Map<String, Object> searchMultipleTermsBenchmark() {
        Map<String, Object> results = new HashMap<>();
        List<String> terms = Arrays.asList("love", "war", "peace");
        for (String term : terms) {
            results.put(term, buildInvertedIndexForTerm(term));
        }
        return results;
    }

    @Benchmark
    public boolean phraseSearchBenchmark() {
        String phrase = "art of war";
        String[] words = phrase.toLowerCase().split("\\s+");
        return words.length > 1;
    }

    private Map<String, List<Integer>> buildInvertedIndexForTerm(String term) {
        Map<String, List<Integer>> index = new HashMap<>();
        String[] tokens = sampleText.toLowerCase()
                .replaceAll("[^a-záéíóúüñ\\s]", "")
                .split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].trim();
            if (word.equals(term)) {
                index.computeIfAbsent(word, k -> new ArrayList<>()).add(i);
            }
        }
        return index;
    }
}
