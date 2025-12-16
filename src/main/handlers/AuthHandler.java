package main.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import main.services.AuthService;
import main.utils.CookieManager;
import main.utils.JsonResponse;
import main.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class AuthHandler extends BaseHandler {
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "POST":
                    if ("/api/auth/register".equals(path)) {
                        handleRegister(exchange);
                    } else if ("/api/auth/login".equals(path)) {
                        handleLogin(exchange);
                    } else if ("/api/auth/logout".equals(path)) {
                        handleLogout(exchange);
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                    break;

                case "GET":
                    if ("/api/auth/me".equals(path)) {
                        handleGetCurrentUser(exchange);
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

    private void handleRegister(HttpExchange exchange) throws IOException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String username = json.get("username").asText();
        String email = json.get("email").asText();
        String password = json.get("password").asText();

        try {
            var user = authService.register(username, email, password);

            // Автоматический логин после регистрации
            String sessionId = SessionManager.createSession(user.getId(), user.getRole());
            CookieManager.setCookie(exchange, "sessionId", sessionId, 24 * 60 * 60);

            Map<String, Object> responseData = Map.of(
                    "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ),
                    "message", "Registration successful"
            );

            sendSuccess(exchange, responseData);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            sendError(exchange, 500, "Database error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonNode json = parseRequestBody(exchange, JsonNode.class);

        String email = json.get("email").asText();
        String password = json.get("password").asText();

        try {
            var user = authService.login(email, password);

            if (user == null) {
                sendError(exchange, 401, "Invalid email or password");
                return;
            }

            String sessionId = SessionManager.createSession(user.getId(), user.getRole());
            CookieManager.setCookie(exchange, "sessionId", sessionId, 24 * 60 * 60);

            Map<String, Object> responseData = Map.of(
                    "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ),
                    "message", "Login successful"
            );

            sendSuccess(exchange, responseData);
        } catch (SQLException e) {
            sendError(exchange, 500, "Database error");
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = CookieManager.getCookie(exchange, "sessionId");
        if (sessionId != null) {
            SessionManager.invalidateSession(sessionId);
            CookieManager.removeCookie(exchange, "sessionId");
        }

        sendSuccess(exchange, "Logout successful");
    }

    private void handleGetCurrentUser(HttpExchange exchange) throws IOException {
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

            Map<String, Object> userData = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "createdAt", user.getCreatedAt().toString()
            );

            sendSuccess(exchange, userData);
        } catch (SQLException e) {
            sendError(exchange, 500, "Database error");
        }
    }
}