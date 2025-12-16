package main.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.utils.CookieManager;
import main.utils.JsonResponse;
import main.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {
    protected final ObjectMapper mapper = new ObjectMapper();

    protected boolean isAuthenticated(HttpExchange exchange) {
        String sessionId = CookieManager.getCookie(exchange, "sessionId");
        return sessionId != null && SessionManager.getUserId(sessionId) != null;
    }

    protected Integer getCurrentUserId(HttpExchange exchange) {
        String sessionId = CookieManager.getCookie(exchange, "sessionId");
        return sessionId != null ? SessionManager.getUserId(sessionId) : null;
    }

    protected String getCurrentUserRole(HttpExchange exchange) {
        String sessionId = CookieManager.getCookie(exchange, "sessionId");
        return sessionId != null ? SessionManager.getUserRole(sessionId) : null;
    }

    protected boolean isAdmin(HttpExchange exchange) {
        String role = getCurrentUserRole(exchange);
        return "ADMIN".equals(role);
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, String response)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message)
            throws IOException {
        String response = JsonResponse.error(message);
        sendResponse(exchange, statusCode, response);
    }

    protected void sendSuccess(HttpExchange exchange, Object data) throws IOException {
        String response = JsonResponse.success(data);
        sendResponse(exchange, 200, response);
    }

    protected void sendSuccess(HttpExchange exchange, String message) throws IOException {
        String response = JsonResponse.success(message);
        sendResponse(exchange, 200, response);
    }

    protected Map<String, String> parseQuery(String query) {
        return java.util.stream.Stream.of(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts.length > 1 ? parts[1] : "",
                        (v1, v2) -> v1
                ));
    }

    protected String getPathParameter(HttpExchange exchange, int index) {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (index < parts.length) {
            return parts[index];
        }
        return null;
    }

    protected <T> T parseRequestBody(HttpExchange exchange, Class<T> valueType)
            throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return mapper.readValue(body, valueType);
    }
}