package main.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import main.services.MovieService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MovieHandler extends BaseHandler {
    private final MovieService movieService = new MovieService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if (path.matches("/api/movies/\\d+")) {
                        // GET /api/movies/{id}
                        handleGetMovie(exchange);
                    } else if ("/api/movies".equals(path)) {
                        // GET /api/movies
                        handleGetMovies(exchange);
                    } else if ("/api/movies/search".equals(path)) {
                        // GET /api/movies/search
                        handleSearchMovies(exchange);
                    } else if ("/api/movies/top".equals(path)) {
                        // GET /api/movies/top
                        handleGetTopMovies(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "POST":
                    if ("/api/movies".equals(path)) {
                        // POST /api/movies (admin only)
                        handleCreateMovie(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "PUT":
                    if (path.matches("/api/movies/\\d+")) {
                        // PUT /api/movies/{id} (admin only)
                        handleUpdateMovie(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "DELETE":
                    if (path.matches("/api/movies/\\d+")) {
                        // DELETE /api/movies/{id} (admin only)
                        handleDeleteMovie(exchange);
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

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

        String sortBy = params.get("sortBy");
        String order = params.get("order");
        String limitStr = params.get("limit");

        Integer limit = null;
        if (limitStr != null && !limitStr.isEmpty()) {
            try {
                limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException e) {
                // Игнорируем неверный лимит
            }
        }

        try {
            List<Map<String, Object>> movies = movieService.getAllMovies(sortBy, order, limit)
                    .stream()
                    .map(this::mapMovieToResponse)
                    .toList();

            sendSuccess(exchange, movies);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get movies");
        }
    }

    private void handleGetMovie(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        int movieId = Integer.parseInt(parts[3]);

        try {
            var movie = movieService.getMovieById(movieId);

            if (movie == null) {
                sendError(exchange, 404, "Movie not found");
                return;
            }

            Map<String, Object> response = mapMovieToDetailedResponse(movie);
            sendSuccess(exchange, response);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get movie");
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid movie ID");
        }
    }

    private void handleSearchMovies(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String query = params.get("q");

        if (query == null || query.trim().isEmpty()) {
            sendError(exchange, 400, "Search query is required");
            return;
        }

        try {
            List<Map<String, Object>> movies = movieService.searchMovies(query)
                    .stream()
                    .map(this::mapMovieToResponse)
                    .toList();

            sendSuccess(exchange, movies);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to search movies");
        }
    }

    private void handleGetTopMovies(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String limitStr = params.get("limit") != null ? params.get("limit") : "10";

        try {
            int limit = Integer.parseInt(limitStr);
            List<Map<String, Object>> movies = movieService.getTopRatedMovies(limit)
                    .stream()
                    .map(this::mapMovieToResponse)
                    .toList();

            sendSuccess(exchange, movies);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid limit parameter");
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get top movies");
        }
    }

    private void handleCreateMovie(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String title = json.get("title").asText();
        String director = json.get("director").asText();
        int year = json.get("year").asInt();
        String description = json.get("description").asText();
        int duration = json.get("duration").asInt();
        String posterUrl = json.has("posterUrl") ? json.get("posterUrl").asText() : null;

        try {
            var movie = movieService.createMovie(title, director, year, description, duration, posterUrl);

            // Добавляем жанры, если указаны
            if (json.has("genres")) {
                List<String> genres = mapper.convertValue(json.get("genres"),
                        mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                movieService.addGenresToMovie(movie.getId(), genres);
            }

            sendSuccess(exchange, Map.of(
                    "message", "Movie created successfully",
                    "movieId", movie.getId()
            ));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to create movie");
        }
    }

    private void handleUpdateMovie(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int movieId = Integer.parseInt(parts[3]);

        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String title = json.get("title").asText();
        String director = json.get("director").asText();
        int year = json.get("year").asInt();
        String description = json.get("description").asText();
        int duration = json.get("duration").asInt();
        String posterUrl = json.has("posterUrl") ? json.get("posterUrl").asText() : null;

        try {
            boolean success = movieService.updateMovie(movieId, title, director, year,
                    description, duration, posterUrl);

            if (success) {
                sendSuccess(exchange, "Movie updated successfully");
            } else {
                sendError(exchange, 404, "Movie not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to update movie");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            sendError(exchange, 403, "Admin access required");
            return;
        }

        String[] parts = exchange.getRequestURI().getPath().split("/");
        int movieId = Integer.parseInt(parts[3]);

        try {
            boolean success = movieService.deleteMovie(movieId);

            if (success) {
                sendSuccess(exchange, "Movie deleted successfully");
            } else {
                sendError(exchange, 404, "Movie not found");
            }
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to delete movie");
        }
    }

    private Map<String, Object> mapMovieToResponse(main.models.Movie movie) {
        return Map.of(
                "id", movie.getId(),
                "title", movie.getTitle(),
                "director", movie.getDirector(),
                "year", movie.getYear(),
                "description", movie.getDescription(),
                "duration", movie.getDuration(),
                "posterUrl", movie.getPosterUrl(),
                "averageRating", movie.getAverageRating()
        );
    }

    private Map<String, Object> mapMovieToDetailedResponse(main.models.Movie movie) {
        List<Map<String, Object>> genres = movie.getGenres().stream()
                .map(genre -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", genre.getId());
                    map.put("name", genre.getName());
                    return map;
                })
                .toList();

        List<Map<String, Object>> reviews = movie.getReviews().stream()
                .map(review -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", review.getId());
                    map.put("userId", review.getUserId());
                    map.put("username", review.getUsername());
                    map.put("rating", review.getRating());
                    map.put("comment", review.getComment());
                    map.put("createdAt", review.getCreatedAt().toString());
                    return map;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("id", movie.getId());
        response.put("title", movie.getTitle());
        response.put("director", movie.getDirector());
        response.put("year", movie.getYear());
        response.put("description", movie.getDescription());
        response.put("duration", movie.getDuration());
        response.put("posterUrl", movie.getPosterUrl());
        response.put("averageRating", movie.getAverageRating());
        response.put("genres", genres);
        response.put("reviews", reviews);

        return response;
    }
}