package main.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import main.services.AdminService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminHandler extends BaseHandler {
    private final AdminService adminService = new AdminService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isAdmin(exchange)) {
                sendError(exchange, 403, "Admin access required");
                return;
            }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Определяем какой endpoint вызывается
            if (path.startsWith("/api/admin/")) {
                String[] parts = path.split("/");

                if (parts.length >= 4) {
                    String resource = parts[3];

                    switch (method) {
                        case "GET":
                            handleGetRequest(exchange, resource, parts);
                            break;
                        case "POST":
                            handlePostRequest(exchange, resource, parts);
                            break;
                        case "PUT":
                            handlePutRequest(exchange, resource, parts);
                            break;
                        case "DELETE":
                            handleDeleteRequest(exchange, resource, parts);
                            break;
                        case "PATCH":
                            handlePatchRequest(exchange, resource, parts);
                            break;
                        default:
                            sendError(exchange, 405, "Method not allowed");
                    }
                } else {
                    sendError(exchange, 404, "Resource not found");
                }
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetRequest(HttpExchange exchange, String resource, String[] parts)
            throws IOException, SQLException {

        switch (resource) {
            case "dashboard":
                handleGetDashboardStats(exchange);
                break;
            case "stats":
                handleGetStats(exchange);
                break;
            case "users":
                if (parts.length > 4 && parts[4].equals("search")) {
                    handleSearchUsers(exchange);
                } else {
                    handleGetUsers(exchange);
                }
                break;
            case "movies":
                if (parts.length > 4 && parts[4].equals("search")) {
                    handleSearchAdminMovies(exchange);
                } else {
                    handleGetAdminMovies(exchange);
                }
                break;
            case "reviews":
                if (parts.length > 4) {
                    if (parts[4].equals("pending")) {
                        handleGetPendingReviews(exchange);
                    } else if (parts[4].equals("reported")) {
                        handleGetReportedReviews(exchange);
                    }
                } else {
                    handleGetAllReviews(exchange);
                }
                break;
            case "genres":
                handleGetGenres(exchange);
                break;
            case "activity":
                handleGetRecentActivity(exchange);
                break;
            case "system":
                handleGetSystemInfo(exchange);
                break;
            case "backup":
                handleCreateBackup(exchange);
                break;
            default:
                sendError(exchange, 404, "Resource not found");
        }
    }

    private void handlePostRequest(HttpExchange exchange, String resource, String[] parts)
            throws IOException, SQLException {

        switch (resource) {
            case "movies":
                handleCreateMovie(exchange);
                break;
            case "genres":
                handleCreateGenre(exchange);
                break;
            case "backup":
                handleCreateBackup(exchange);
                break;
            default:
                sendError(exchange, 404, "Resource not found");
        }
    }

    private void handlePutRequest(HttpExchange exchange, String resource, String[] parts)
            throws IOException, SQLException {

        if (parts.length < 5) {
            sendError(exchange, 400, "ID required");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid ID");
            return;
        }

        switch (resource) {
            case "users":
                handleUpdateUser(exchange, id);
                break;
            case "movies":
                handleUpdateMovie(exchange, id);
                break;
            case "genres":
                handleUpdateGenre(exchange, id);
                break;
            default:
                sendError(exchange, 404, "Resource not found");
        }
    }

    private void handleDeleteRequest(HttpExchange exchange, String resource, String[] parts)
            throws IOException, SQLException {

        if (parts.length < 5) {
            sendError(exchange, 400, "ID required");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid ID");
            return;
        }

        switch (resource) {
            case "users":
                handleDeleteUser(exchange, id);
                break;
            case "movies":
                handleDeleteMovie(exchange, id);
                break;
            case "genres":
                handleDeleteGenre(exchange, id);
                break;
            case "reviews":
                handleDeleteReview(exchange, id);
                break;
            default:
                sendError(exchange, 404, "Resource not found");
        }
    }

    private void handlePatchRequest(HttpExchange exchange, String resource, String[] parts)
            throws IOException, SQLException {

        if (parts.length < 5) {
            sendError(exchange, 400, "ID required");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid ID");
            return;
        }

        switch (resource) {
            case "reviews":
                if (parts.length > 5 && "approve".equals(parts[5])) {
                    handleApproveReview(exchange, id);
                } else if (parts.length > 5 && "reject".equals(parts[5])) {
                    handleRejectReview(exchange, id);
                } else {
                    sendError(exchange, 404, "Action not found");
                }
                break;
            case "users":
                if (parts.length > 5 && "status".equals(parts[5])) {
                    handleUpdateUserStatus(exchange, id);
                } else if (parts.length > 5 && "role".equals(parts[5])) {
                    handleUpdateUserRole(exchange, id);
                } else {
                    sendError(exchange, 404, "Action not found");
                }
                break;
            default:
                sendError(exchange, 404, "Resource not found");
        }
    }

    // === Обработчики конкретных запросов ===

    private void handleGetDashboardStats(HttpExchange exchange) throws IOException, SQLException {
        var stats = adminService.getDashboardStats();
        sendSuccess(exchange, stats);
    }

    private void handleGetStats(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String yearStr = params.get("year");

        int year = yearStr != null ? Integer.parseInt(yearStr) : java.time.Year.now().getValue();
        var monthlyStats = adminService.getMonthlyStats(year);

        sendSuccess(exchange, Map.of(
                "year", year,
                "monthlyStats", monthlyStats
        ));
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String pageStr = params.get("page");
        String limitStr = params.get("limit");
        String filter = params.get("filter");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        var users = adminService.getUsers(page, limit, filter);
        var total = adminService.getUsersCount(filter);

        sendSuccess(exchange, Map.of(
                "users", users,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", total,
                        "pages", (int) Math.ceil((double) total / limit)
                )
        ));
    }

    private void handleSearchUsers(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String query = params.get("q");

        if (query == null || query.trim().isEmpty()) {
            sendError(exchange, 400, "Search query required");
            return;
        }

        var users = adminService.searchUsers(query);
        sendSuccess(exchange, users);
    }

    private void handleGetAdminMovies(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String pageStr = params.get("page");
        String limitStr = params.get("limit");
        String sortBy = params.get("sortBy");
        String order = params.get("order");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        var movies = adminService.getMovies(page, limit, sortBy, order);
        var total = adminService.getMoviesCount();

        sendSuccess(exchange, Map.of(
                "movies", movies,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", total,
                        "pages", (int) Math.ceil((double) total / limit)
                )
        ));
    }

    private void handleSearchAdminMovies(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String query = params.get("q");

        if (query == null || query.trim().isEmpty()) {
            sendError(exchange, 400, "Search query required");
            return;
        }

        var movies = adminService.searchMovies(query);
        sendSuccess(exchange, movies);
    }

    private void handleGetAllReviews(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String pageStr = params.get("page");
        String limitStr = params.get("limit");
        String filter = params.get("filter");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        var reviews = adminService.getReviews(page, limit, filter);
        var total = adminService.getReviewsCount(filter);

        sendSuccess(exchange, Map.of(
                "reviews", reviews,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", total,
                        "pages", (int) Math.ceil((double) total / limit)
                )
        ));
    }

    private void handleGetPendingReviews(HttpExchange exchange) throws IOException, SQLException {
        var reviews = adminService.getPendingReviews();
        sendSuccess(exchange, reviews);
    }

    private void handleGetReportedReviews(HttpExchange exchange) throws IOException, SQLException {
        var reviews = adminService.getReportedReviews();
        sendSuccess(exchange, reviews);
    }

    private void handleGetGenres(HttpExchange exchange) throws IOException, SQLException {
        var genres = adminService.getGenresWithStats();
        sendSuccess(exchange, genres);
    }

    private void handleGetRecentActivity(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String limitStr = params.get("limit");

        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;
        var activity = adminService.getRecentActivity(limit);

        sendSuccess(exchange, activity);
    }

    private void handleGetSystemInfo(HttpExchange exchange) throws IOException, SQLException {
        var info = adminService.getSystemInfo();
        sendSuccess(exchange, info);
    }

    private void handleCreateMovie(HttpExchange exchange) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String title = json.get("title").asText();
        String director = json.has("director") ? json.get("director").asText() : null;
        int year = json.get("year").asInt();
        String description = json.has("description") ? json.get("description").asText() : null;
        Integer duration = json.has("duration") ? json.get("duration").asInt() : null;
        String posterUrl = json.has("posterUrl") ? json.get("posterUrl").asText() : null;

        List<String> genres = null;
        if (json.has("genres")) {
            genres = mapper.convertValue(json.get("genres"),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }

        var movie = adminService.createMovie(title, director, year, description, duration, posterUrl, genres);

        sendSuccess(exchange, Map.of(
                "message", "Movie created successfully",
                "movieId", movie.getId()
        ));
    }

    private void handleUpdateMovie(HttpExchange exchange, int movieId) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String title = json.get("title").asText();
        String director = json.has("director") ? json.get("director").asText() : null;
        int year = json.get("year").asInt();
        String description = json.has("description") ? json.get("description").asText() : null;
        Integer duration = json.has("duration") ? json.get("duration").asInt() : null;
        String posterUrl = json.has("posterUrl") ? json.get("posterUrl").asText() : null;

        boolean success = adminService.updateMovie(movieId, title, director, year, description, duration, posterUrl);

        if (success) {
            sendSuccess(exchange, "Movie updated successfully");
        } else {
            sendError(exchange, 404, "Movie not found");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, int movieId) throws IOException, SQLException {
        boolean success = adminService.deleteMovie(movieId);

        if (success) {
            sendSuccess(exchange, "Movie deleted successfully");
        } else {
            sendError(exchange, 404, "Movie not found");
        }
    }

    private void handleCreateGenre(HttpExchange exchange) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);
        String name = json.get("name").asText();

        var genre = adminService.createGenre(name);

        sendSuccess(exchange, Map.of(
                "message", "Genre created successfully",
                "genreId", genre.getId()
        ));
    }

    private void handleUpdateGenre(HttpExchange exchange, int genreId) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);
        String name = json.get("name").asText();

        boolean success = adminService.updateGenre(genreId, name);

        if (success) {
            sendSuccess(exchange, "Genre updated successfully");
        } else {
            sendError(exchange, 404, "Genre not found");
        }
    }

    private void handleDeleteGenre(HttpExchange exchange, int genreId) throws IOException, SQLException {
        boolean success = adminService.deleteGenre(genreId);

        if (success) {
            sendSuccess(exchange, "Genre deleted successfully");
        } else {
            sendError(exchange, 404, "Genre not found");
        }
    }

    private void handleUpdateUser(HttpExchange exchange, int userId) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String username = json.get("username").asText();
        String email = json.get("email").asText();
        String role = json.get("role").asText();
        boolean isActive = json.get("isActive").asBoolean();

        boolean success = adminService.updateUser(userId, username, email, role, isActive);

        if (success) {
            sendSuccess(exchange, "User updated successfully");
        } else {
            sendError(exchange, 404, "User not found");
        }
    }

    private void handleDeleteUser(HttpExchange exchange, int userId) throws IOException, SQLException {
        boolean success = adminService.deleteUser(userId);

        if (success) {
            sendSuccess(exchange, "User deleted successfully");
        } else {
            sendError(exchange, 404, "User not found");
        }
    }

    private void handleUpdateUserStatus(HttpExchange exchange, int userId) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);
        boolean isActive = json.get("isActive").asBoolean();

        boolean success = adminService.updateUserStatus(userId, isActive);

        if (success) {
            sendSuccess(exchange, "User status updated successfully");
        } else {
            sendError(exchange, 404, "User not found");
        }
    }

    private void handleUpdateUserRole(HttpExchange exchange, int userId) throws IOException, SQLException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);
        String role = json.get("role").asText();

        boolean success = adminService.updateUserRole(userId, role);

        if (success) {
            sendSuccess(exchange, "User role updated successfully");
        } else {
            sendError(exchange, 404, "User not found");
        }
    }

    private void handleDeleteReview(HttpExchange exchange, int reviewId) throws IOException, SQLException {
        boolean success = adminService.deleteReview(reviewId);

        if (success) {
            sendSuccess(exchange, "Review deleted successfully");
        } else {
            sendError(exchange, 404, "Review not found");
        }
    }

    private void handleApproveReview(HttpExchange exchange, int reviewId) throws IOException, SQLException {
        boolean success = adminService.approveReview(reviewId);

        if (success) {
            sendSuccess(exchange, "Review approved successfully");
        } else {
            sendError(exchange, 404, "Review not found");
        }
    }

    private void handleRejectReview(HttpExchange exchange, int reviewId) throws IOException, SQLException {
        boolean success = adminService.rejectReview(reviewId);

        if (success) {
            sendSuccess(exchange, "Review rejected successfully");
        } else {
            sendError(exchange, 404, "Review not found");
        }
    }

    private void handleCreateBackup(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String backupPath = params.get("path");

        if (backupPath == null || backupPath.trim().isEmpty()) {
            backupPath = "backups";
        }

        boolean success = adminService.createBackup(backupPath);

        if (success) {
            sendSuccess(exchange, Map.of(
                    "message", "Backup created successfully",
                    "path", backupPath
            ));
        } else {
            sendError(exchange, 500, "Failed to create backup");
        }
    }
}