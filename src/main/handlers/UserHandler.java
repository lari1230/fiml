package main.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import main.services.AuthService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class UserHandler extends BaseHandler {
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if ("/api/user/profile".equals(path)) {
                        handleGetProfile(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "PUT":
                    if ("/api/user/profile".equals(path)) {
                        handleUpdateProfile(exchange);
                    } else if ("/api/user/password".equals(path)) {
                        handleChangePassword(exchange);
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

    private void handleGetProfile(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        Integer userId = getCurrentUserId(exchange);

        try {
            var user = authService.getUserById(userId);

            if (user == null) {
                sendError(exchange, 404, "User not found");
                return;
            }

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "createdAt", user.getCreatedAt().toString(),
                    "isActive", user.isActive()
            );

            sendSuccess(exchange, response);
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to get profile");
        }
    }

    private void handleUpdateProfile(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        Integer userId = getCurrentUserId(exchange);
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String username = json.get("username").asText();
        String email = json.get("email").asText();

        // Здесь должна быть логика обновления профиля
        // В реальном приложении нужно добавить метод updateUser в UserDAO

        sendSuccess(exchange, "Profile update functionality not implemented yet");
    }

    private void handleChangePassword(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            sendError(exchange, 401, "Not authenticated");
            return;
        }

        Integer userId = getCurrentUserId(exchange);
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String oldPassword = json.get("oldPassword").asText();
        String newPassword = json.get("newPassword").asText();

        try {
            boolean success = authService.changePassword(userId, oldPassword, newPassword);

            if (success) {
                sendSuccess(exchange, "Password changed successfully");
            } else {
                sendError(exchange, 400, "Invalid old password");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Failed to change password");
        }
    }
}