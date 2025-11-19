package es.ulpgc.searchengine.search;

import es.ulpgc.searchengine.search.repository.DatamartSQLite;
import java.util.*;

public class AdvancedSearchEngine {

    private final DatamartSQLite repo;

    public AdvancedSearchEngine(DatamartSQLite repo) {
        this.repo = repo;
    }

    public List<Map<String,Object>> search(String term, String author, String language, Integer year) {
        term = term.toLowerCase();
        List<Map<String,Object>> books = repo.allBooks();
        List<Map<String,Object>> results = new ArrayList<>();

        for (var b : books) {
            String content = b.get("content").toString().toLowerCase();
            if (!content.contains(term)) continue;

            if (author != null && !author.isBlank())
                if (!b.get("author").toString().toLowerCase().contains(author.toLowerCase()))
                    continue;

            if (language != null && !language.isBlank())
                if (!b.get("language").toString().equalsIgnoreCase(language))
                    continue;

            if (year != null)
                if (!b.get("year").toString().equals(String.valueOf(year)))
                    continue;

            results.add(b);
        }

        return results;
    }

    public List<Map<String,Object>> searchPhrase(String phrase, String author, String language, Integer year) {
        return search(phrase, author, language, year);
    }

    public List<Map<String,Object>> booleanSearch(String query, String author, String language, Integer year) {
        String[] terms = query.toLowerCase().split("and");
        List<Map<String,Object>> result = search(terms[0].trim(), author, language, year);

        for (int i = 1; i < terms.length; i++) {
            List<Map<String,Object>> next = search(terms[i].trim(), author, language, year);
            result.retainAll(next);
        }

        return result;
    }

    public List<Map<String,Object>> searchByYearRange(int start, int end, String term) {
        List<Map<String,Object>> raw = repo.queryByYearRange(start, end);

        if (term == null || term.isBlank()) return raw;

        List<Map<String,Object>> out = new ArrayList<>();
        for (var b : raw) {
            if (b.get("content").toString().toLowerCase().contains(term.toLowerCase()))
                out.add(b);
        }
        return out;
    }

    public Map<String,Object> getSearchStats() {
        Map<String,Object> stats = new HashMap<>();
        stats.put("books", repo.allBooks().size());
        stats.put("status", "ok");
        return stats;
    }

    public void refreshCache() {
        // optional
    }
}
