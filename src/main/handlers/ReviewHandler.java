package main.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import main.services.ReviewService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ReviewHandler extends BaseHandler {
    private final ReviewService reviewService = new ReviewService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if (path.matches("/api/reviews/movie/\\d+")) {
                        // GET /api/reviews/movie/{movieId}
                        handleGetMovieReviews(exchange);
                    } else if (path.matches("/api/reviews/\\d+")) {
                        // GET /api/reviews/{reviewId}
                        handleGetReview(exchange);
                    } else if ("/api/reviews/my".equals(path)) {
                        // GET /api/reviews/my
                        handleGetMyReviews(exchange);
                    } else if ("/api/reviews/pending".equals(path)) {
                        // GET /api/reviews/pending (admin only)
                        handleGetPendingReviews(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "POST":
                    if ("/api/reviews".equals(path)) {
                        // POST /api/reviews
                        handleCreateReview(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "PUT":
                    if (path.matches("/api/reviews/\\d+")) {
                        // PUT /api/reviews/{reviewId}
                        handleUpdateReview(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "DELETE":
                    if (path.matches("/api/reviews/\\d+")) {
                        // DELETE /api/reviews/{reviewId}
                        handleDeleteReview(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "PATCH":
                    if (path.matches("/api/reviews/\\d+/approve")) {
                        // PATCH /api/reviews/{reviewId}/approve (admin only)
                        handleApproveReview(exchange);
                    } else if (path.matches("/api/reviews/\\d+/reject")) {
                        // PATCH /api/reviews/{reviewId}/reject (admin only)
                        handleRejectReview(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                default:
                    sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetMovieReviews(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        int movieId = Integer.parseInt(parts[3]);

        try {
            List<Map<String, Object>> reviews = reviewService.getMovieReviews(movieId)
                    .stream()
                    .map(this::mapReviewToResponse)
                    .toList();

            Map<String, Object> response = Map.of(
                    "reviews", reviews,
                    "averageRating", reviewService.getAverageRating(movieId),
                    "reviewCount", reviewService.getReviewCount(movieId)
            );

            sendSuccess(exchange, response);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get reviews");
        }
    }

    private void handleGetReview(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        int reviewId = Integer.parseInt(parts[2]);

        try {
            var review = reviewService.getReviewById(reviewId);

            if (review == null) {
                sendError(exchange, 404, "Review not found");
                return;
            }

            sendSuccess(exchange, mapReviewToResponse(review));
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get review");
        }
    }

    private void handleGetMyReviews(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        Integer userId = getCurrentUserId(exchange);

        try {
            List<Map<String, Object>> reviews = reviewService.getUserReviews(userId)
                    .stream()
                    .map(this::mapReviewToResponse)
                    .toList();

            sendSuccess(exchange, reviews);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get reviews");
        }
    }

    private void handleGetPendingReviews(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        try {
            List<Map<String, Object>> reviews = reviewService.getAllReviews(true)
                    .stream()
                    .map(this::mapReviewToResponse)
                    .toList();

            sendSuccess(exchange, reviews);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get pending reviews");
        }
    }

    private void handleCreateReview(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        Integer userId = getCurrentUserId(exchange);
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        int movieId = json.get("movieId").asInt();
        int rating = json.get("rating").asInt();
        String comment = json.get("comment").asText();

        try {
            var review = reviewService.createReview(movieId, userId, rating, comment);
            sendSuccess(exchange, Map.of(
                    "message", "Review created successfully",
                    "reviewId", review.getId()
            ));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            sendError(exchange, 403, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to create review");
        }
    }

    private void handleUpdateReview(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int reviewId = Integer.parseInt(parts[2]);
        Integer userId = getCurrentUserId(exchange);

        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        int rating = json.get("rating").asInt();
        String comment = json.get("comment").asText();

        try {
            boolean success = reviewService.updateReview(reviewId, userId, rating, comment);

            if (success) {
                sendSuccess(exchange, "Review updated successfully");
            } else {
                sendError(exchange, 404, "Review not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            sendError(exchange, 403, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to update review");
        }
    }

    private void handleDeleteReview(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int reviewId = Integer.parseInt(parts[2]);
        Integer userId = getCurrentUserId(exchange);

        try {
            boolean success = reviewService.deleteReview(reviewId, userId);

            if (success) {
                sendSuccess(exchange, "Review deleted successfully");
            } else {
                sendError(exchange, 404, "Review not found");
            }
        } catch (SecurityException e) {
            sendError(exchange, 403, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to delete review");
        }
    }

    private void handleApproveReview(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int reviewId = Integer.parseInt(parts[2]);

        try {
            boolean success = reviewService.approveReview(reviewId);

            if (success) {
                sendSuccess(exchange, "Review approved successfully");
            } else {
                sendError(exchange, 404, "Review not found");
            }
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to approve review");
        }
    }

    private void handleRejectReview(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int reviewId = Integer.parseInt(parts[2]);

        try {
            boolean success = reviewService.rejectReview(reviewId);

            if (success) {
                sendSuccess(exchange, "Review rejected successfully");
            } else {
                sendError(exchange, 404, "Review not found");
            }
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to reject review");
        }
    }

    private Map<String, Object> mapReviewToResponse(main.models.Review review) {
        return Map.of(
                "id", review.getId(),
                "movieId", review.getMovieId(),
                "userId", review.getUserId(),
                "username", review.getUsername() != null ? review.getUsername() : "",
                "movieTitle", review.getMovieTitle() != null ? review.getMovieTitle() : "",
                "rating", review.getRating(),
                "comment", review.getComment(),
                "createdAt", review.getCreatedAt().toString(),
                "updatedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : "",
                "isApproved", review.isApproved()
        );
    }
}