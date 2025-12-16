package main.dao;

import main.models.Movie;
import main.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    public Movie createMovie(Movie movie) throws SQLException {
        String sql = "INSERT INTO movies (title, director, year, description, duration, poster_url) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, movie.getTitle());
            stmt.setString(2, movie.getDirector());
            stmt.setInt(3, movie.getYear());
            stmt.setString(4, movie.getDescription());
            stmt.setInt(5, movie.getDuration());
            stmt.setString(6, movie.getPosterUrl());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating movie failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    movie.setId(generatedKeys.getInt(1));
                }
            }

            return movie;
        }
    }

    public Movie getMovieById(int id) throws SQLException {
        String sql = "SELECT m.*, AVG(r.rating) as avg_rating, COUNT(r.id) as review_count " +
                "FROM movies m " +
                "LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE " +
                "WHERE m.id = ? " +
                "GROUP BY m.id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMovie(rs);
                }
            }
        }
        return null;
    }

    public List<Movie> getAllMovies(String sortBy, String order, Integer limit) throws SQLException {
        List<Movie> movies = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT m.*, AVG(r.rating) as avg_rating, COUNT(r.id) as review_count " +
                        "FROM movies m " +
                        "LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE " +
                        "GROUP BY m.id "
        );

        // Добавляем сортировку
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "rating":
                    sql.append("ORDER BY avg_rating ");
                    break;
                case "year":
                    sql.append("ORDER BY m.year ");
                    break;
                case "title":
                    sql.append("ORDER BY m.title ");
                    break;
                case "reviews":
                    sql.append("ORDER BY review_count ");
                    break;
                default:
                    sql.append("ORDER BY m.created_at ");
            }

            sql.append("DESC".equalsIgnoreCase(order) ? "DESC" : "ASC");
        } else {
            sql.append("ORDER BY m.created_at DESC");
        }

        // Добавляем лимит
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                movies.add(mapResultSetToMovie(rs));
            }
        }
        return movies;
    }

    public List<Movie> searchMovies(String query) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.*, AVG(r.rating) as avg_rating " +
                "FROM movies m " +
                "LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE " +
                "WHERE m.title LIKE ? OR m.director LIKE ? OR m.description LIKE ? " +
                "GROUP BY m.id " +
                "ORDER BY m.title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        }
        return movies;
    }

    public boolean updateMovie(Movie movie) throws SQLException {
        String sql = "UPDATE movies SET title = ?, director = ?, year = ?, " +
                "description = ?, duration = ?, poster_url = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, movie.getTitle());
            stmt.setString(2, movie.getDirector());
            stmt.setInt(3, movie.getYear());
            stmt.setString(4, movie.getDescription());
            stmt.setInt(5, movie.getDuration());
            stmt.setString(6, movie.getPosterUrl());
            stmt.setInt(7, movie.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteMovie(int movieId) throws SQLException {
        String sql = "DELETE FROM movies WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Movie> getMoviesByYearRange(int fromYear, int toYear) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.*, AVG(r.rating) as avg_rating " +
                "FROM movies m " +
                "LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE " +
                "WHERE m.year BETWEEN ? AND ? " +
                "GROUP BY m.id " +
                "ORDER BY m.year DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fromYear);
            stmt.setInt(2, toYear);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        }
        return movies;
    }

    public List<Movie> getTopRatedMovies(int limit) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.*, AVG(r.rating) as avg_rating " +
                "FROM movies m " +
                "LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE " +
                "GROUP BY m.id " +
                "HAVING avg_rating IS NOT NULL " +
                "ORDER BY avg_rating DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        }
        return movies;
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setId(rs.getInt("id"));
        movie.setTitle(rs.getString("title"));
        movie.setDirector(rs.getString("director"));
        movie.setYear(rs.getInt("year"));
        movie.setDescription(rs.getString("description"));
        movie.setDuration(rs.getInt("duration"));
        movie.setPosterUrl(rs.getString("poster_url"));

        double avgRating = rs.getDouble("avg_rating");
        if (!rs.wasNull()) {
            movie.setAverageRating(Math.round(avgRating * 10.0) / 10.0);
        }

        return movie;
    }
}