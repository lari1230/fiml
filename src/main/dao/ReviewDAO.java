package main.dao;

import main.models.Review;
import main.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public Review createReview(Review review) throws SQLException {
        String sql = "INSERT INTO reviews (movie_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, review.getMovieId());
            stmt.setInt(2, review.getUserId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getComment());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating review failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    review.setId(generatedKeys.getInt(1));
                }
            }

            return review;
        }
    }

    public Review getReviewById(int id) throws SQLException {
        String sql = "SELECT r.*, u.username, m.title as movie_title " +
                "FROM reviews r " +
                "JOIN users u ON r.user_id = u.id " +
                "JOIN movies m ON r.movie_id = m.id " +
                "WHERE r.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReview(rs);
                }
            }
        }
        return null;
    }

    public Review getReviewByUserAndMovie(int userId, int movieId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE user_id = ? AND movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReview(rs);
                }
            }
        }
        return null;
    }

    public List<Review> getMovieReviews(int movieId, boolean includeUnapproved) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, u.username FROM reviews r " +
                "JOIN users u ON r.user_id = u.id " +
                "WHERE r.movie_id = ? ";

        if (!includeUnapproved) {
            sql += "AND r.is_approved = TRUE ";
        }

        sql += "ORDER BY r.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        return reviews;
    }

    public List<Review> getUserReviews(int userId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, m.title as movie_title FROM reviews r " +
                "JOIN movies m ON r.movie_id = m.id " +
                "WHERE r.user_id = ? " +
                "ORDER BY r.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        return reviews;
    }

    public List<Review> getAllReviews(boolean onlyUnapproved) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, u.username, m.title as movie_title FROM reviews r " +
                "JOIN users u ON r.user_id = u.id " +
                "JOIN movies m ON r.movie_id = m.id ";

        if (onlyUnapproved) {
            sql += "WHERE r.is_approved = FALSE ";
        }

        sql += "ORDER BY r.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        }
        return reviews;
    }

    public boolean updateReview(Review review) throws SQLException {
        String sql = "UPDATE reviews SET rating = ?, comment = ?, is_approved = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, review.getRating());
            stmt.setString(2, review.getComment());
            stmt.setBoolean(3, review.isApproved());
            stmt.setInt(4, review.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteReview(int reviewId) throws SQLException {
        String sql = "DELETE FROM reviews WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean approveReview(int reviewId) throws SQLException {
        String sql = "UPDATE reviews SET is_approved = TRUE WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean rejectReview(int reviewId) throws SQLException {
        String sql = "DELETE FROM reviews WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            return stmt.executeUpdate() > 0;
        }
    }

    public double getAverageRating(int movieId) throws SQLException {
        String sql = "SELECT AVG(rating) as avg_rating FROM reviews " +
                "WHERE movie_id = ? AND is_approved = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rating");
                }
            }
        }
        return 0.0;
    }

    public int getReviewCount(int movieId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM reviews " +
                "WHERE movie_id = ? AND is_approved = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getInt("id"));
        review.setMovieId(rs.getInt("movie_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setCreatedAt(rs.getTimestamp("created_at"));
        review.setUpdatedAt(rs.getTimestamp("updated_at"));
        review.setApproved(rs.getBoolean("is_approved"));

        try {
            review.setUsername(rs.getString("username"));
        } catch (SQLException e) {
            // Игнорируем, если столбца нет
        }

        try {
            review.setMovieTitle(rs.getString("movie_title"));
        } catch (SQLException e) {
            // Игнорируем, если столбца нет
        }

        return review;
    }
}