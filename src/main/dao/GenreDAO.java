package main.dao;

import main.models.Genre;
import main.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GenreDAO {

    public List<Genre> getAllGenres() throws SQLException {
        List<Genre> genres = new ArrayList<>();
        String sql = "SELECT * FROM genres ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                genres.add(genre);
            }
        }
        return genres;
    }

    public Genre getGenreById(int id) throws SQLException {
        String sql = "SELECT * FROM genres WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Genre genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                    return genre;
                }
            }
        }
        return null;
    }

    public Genre createGenre(String name) throws SQLException {
        String sql = "INSERT INTO genres (name) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating genre failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Genre genre = new Genre(name);
                    genre.setId(generatedKeys.getInt(1));
                    return genre;
                }
            }
        }
        return null;
    }

    public List<Genre> getMovieGenres(int movieId) throws SQLException {
        List<Genre> genres = new ArrayList<>();
        String sql = "SELECT g.* FROM genres g " +
                "JOIN movie_genres mg ON g.id = mg.genre_id " +
                "WHERE mg.movie_id = ? " +
                "ORDER BY g.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Genre genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                    genres.add(genre);
                }
            }
        }
        return genres;
    }

    public boolean addGenreToMovie(int movieId, int genreId) throws SQLException {
        String sql = "INSERT IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, genreId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeGenreFromMovie(int movieId, int genreId) throws SQLException {
        String sql = "DELETE FROM movie_genres WHERE movie_id = ? AND genre_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, genreId);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Genre> getGenresByNames(List<String> genreNames) throws SQLException {
        List<Genre> genres = new ArrayList<>();
        if (genreNames.isEmpty()) return genres;

        StringBuilder sql = new StringBuilder("SELECT * FROM genres WHERE name IN (");
        for (int i = 0; i < genreNames.size(); i++) {
            sql.append("?");
            if (i < genreNames.size() - 1) sql.append(", ");
        }
        sql.append(")");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < genreNames.size(); i++) {
                stmt.setString(i + 1, genreNames.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Genre genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                    genres.add(genre);
                }
            }
        }
        return genres;
    }
}