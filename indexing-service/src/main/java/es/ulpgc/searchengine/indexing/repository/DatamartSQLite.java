package es.ulpgc.searchengine.indexing.repository;

import java.sql.*;
import java.util.*;

public class DatamartSQLite {

    private final String dbPath;
    public DatamartSQLite(String dbPath) {
        this.dbPath = dbPath;
    }

    public void initSchema() {
        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    book_id INTEGER PRIMARY KEY,
                    title TEXT,
                    author TEXT,
                    language TEXT,
                    year INTEGER,
                    content TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS inverted_index (
                    term TEXT,
                    book_id INTEGER,
                    positions TEXT,
                    PRIMARY KEY(term, book_id)
                );
            """);

            System.out.println("[Datamart] Schema initialized");
        } catch (SQLException e) {
            System.err.println("[Datamart] Schema error: " + e.getMessage());
        }
    }

    public void insertOrUpdateBook(int bookId, String title, String author, String language, int year, String content) {
        String sql = """
            INSERT INTO books (book_id, title, author, language, year, content)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(book_id) DO UPDATE SET
                title = excluded.title,
                author = excluded.author,
                language = excluded.language,
                year = excluded.year,
                content = excluded.content;
        """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setString(4, language);
            ps.setInt(5, year);
            ps.setString(6, content);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Datamart] Error inserting/updating book: " + e.getMessage());
        }
    }

    /* -----------------------------------------
       Delete old index entries for a book
       ----------------------------------------- */
    public void deleteIndexForBook(int bookId) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM inverted_index WHERE book_id = ?")) {

            ps.setInt(1, bookId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Datamart] Error deleting index: " + e.getMessage());
        }
    }

    public void insertIndex(int bookId, Map<String, List<Integer>> index) {
        String sql = """
            INSERT INTO inverted_index (term, book_id, positions)
            VALUES (?, ?, ?)
            ON CONFLICT(term, book_id) DO UPDATE SET
                positions = excluded.positions;
        """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (var entry : index.entrySet()) {
                String term = entry.getKey();
                List<Integer> positions = entry.getValue();
                String posCsv = positions.toString();  // example: [3,7,20]

                ps.setString(1, term);
                ps.setInt(2, bookId);
                ps.setString(3, posCsv);

                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            System.err.println("[Datamart] Error inserting index: " + e.getMessage());
        }
    }

    public Map<String,Object> getStats() {
        Map<String,Object> stats = new HashMap<>();

        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) AS total FROM books");
            stats.put("books", rs1.getInt("total"));

            ResultSet rs2 = st.executeQuery("SELECT COUNT(*) AS terms FROM inverted_index");
            stats.put("terms_indexed", rs2.getInt("terms"));

        } catch (SQLException e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    public String getBookContent(int bookId) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT content FROM books WHERE book_id = ?")) {

            ps.setInt(1, bookId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("content");

        } catch (SQLException e) {
            System.err.println("[Datamart] Error loading content: " + e.getMessage());
        }
        return null;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
}
