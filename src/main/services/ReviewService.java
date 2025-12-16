package main.services;

import main.dao.ReviewDAO;
import main.dao.MovieDAO;
import main.models.Review;
import main.utils.Validator;
import java.sql.SQLException;
import java.util.List;

public class ReviewService {
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    public Review createReview(int movieId, int userId, int rating, String comment)
            throws SQLException {

        // Проверка существования фильма
        if (movieDAO.getMovieById(movieId) == null) {
            throw new IllegalArgumentException("Фильм не найден");
        }

        // Валидация оценки
        if (!Validator.isValidRating(rating)) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 10");
        }

        // Проверка, не оставлял ли пользователь уже отзыв
        Review existingReview = reviewDAO.getReviewByUserAndMovie(userId, movieId);
        if (existingReview != null) {
            throw new IllegalArgumentException("Вы уже оставляли отзыв на этот фильм");
        }

        // Создание отзыва
        Review review = new Review(movieId, userId, rating, comment);
        return reviewDAO.createReview(review);
    }

    public List<Review> getMovieReviews(int movieId) throws SQLException {
        return reviewDAO.getMovieReviews(movieId, false);
    }

    public List<Review> getUserReviews(int userId) throws SQLException {
        return reviewDAO.getUserReviews(userId);
    }

    public Review getReviewById(int reviewId) throws SQLException {
        return reviewDAO.getReviewById(reviewId);
    }

    public boolean updateReview(int reviewId, int userId, int rating, String comment)
            throws SQLException {

        Review review = reviewDAO.getReviewById(reviewId);

        if (review == null) {
            return false;
        }

        // Проверка прав доступа
        if (review.getUserId() != userId) {
            throw new SecurityException("Вы можете редактировать только свои отзывы");
        }

        // Валидация оценки
        if (!Validator.isValidRating(rating)) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 10");
        }

        review.setRating(rating);
        review.setComment(comment);

        return reviewDAO.updateReview(review);
    }

    public boolean deleteReview(int reviewId, int userId) throws SQLException {
        Review review = reviewDAO.getReviewById(reviewId);

        if (review == null) {
            return false;
        }

        // Проверка прав доступа
        if (review.getUserId() != userId) {
            throw new SecurityException("Вы можете удалять только свои отзывы");
        }

        return reviewDAO.deleteReview(reviewId);
    }

    public double getAverageRating(int movieId) throws SQLException {
        return reviewDAO.getAverageRating(movieId);
    }

    public int getReviewCount(int movieId) throws SQLException {
        return reviewDAO.getReviewCount(movieId);
    }

    public List<Review> getAllReviews(boolean onlyUnapproved) throws SQLException {
        return reviewDAO.getAllReviews(onlyUnapproved);
    }

    public boolean approveReview(int reviewId) throws SQLException {
        return reviewDAO.approveReview(reviewId);
    }

    public boolean rejectReview(int reviewId) throws SQLException {
        return reviewDAO.rejectReview(reviewId);
    }
}