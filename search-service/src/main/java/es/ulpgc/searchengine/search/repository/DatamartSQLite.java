package es.ulpgc.searchengine.search.repository;

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

            System.out.println("Datamart schema initialized");
        } catch (SQLException e) {
            System.err.println("Error initializing schema: " + e.getMessage());
        }
    }

    public List<Map<String,Object>> allBooks() {
        List<Map<String,Object>> list = new ArrayList<>();

        String sql = "SELECT book_id, title, author, language, year, content FROM books";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                row.put("book_id", rs.getInt("book_id"));
                row.put("title", rs.getString("title"));
                row.put("author", rs.getString("author"));
                row.put("language", rs.getString("language"));
                row.put("year", rs.getInt("year"));
                row.put("content", rs.getString("content"));
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("Error reading books: " + e.getMessage());
        }

        return list;
    }

    public List<Map<String,Object>> queryByYearRange(int startYear, int endYear) {
        List<Map<String,Object>> list = new ArrayList<>();

        String sql = """
            SELECT book_id, title, author, language, year, content
            FROM books
            WHERE year >= ? AND year <= ?
        """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, startYear);
            ps.setInt(2, endYear);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("book_id", rs.getInt("book_id"));
                    row.put("title", rs.getString("title"));
                    row.put("author", rs.getString("author"));
                    row.put("language", rs.getString("language"));
                    row.put("year", rs.getInt("year"));
                    row.put("content", rs.getString("content"));
                    list.add(row);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error in queryByYearRange: " + e.getMessage());
        }

        return list;
    }

    public List<Map<String,Object>> queryRaw(String sql) {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int columns = md.getColumnCount();

            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                for (int i = 1; i <= columns; i++) {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("Error in queryRaw: " + e.getMessage());
        }
        return list;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
}
