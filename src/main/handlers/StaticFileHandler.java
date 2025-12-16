package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
    private static final String WEB_ROOT = "src/main/resources/web";
    private static final String DEFAULT_FILE = "index.html";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!"GET".equals(method)) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Если путь заканчивается на "/", добавляем index.html
        if (path.endsWith("/")) {
            path += DEFAULT_FILE;
        }

        // Если путь пустой или "/", отдаем index.html
        if (path.equals("") || path.equals("/")) {
            path = "/" + DEFAULT_FILE;
        }

        // Декодируем URL
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);

        // Определяем файл на основе пути
        String filePath = WEB_ROOT + path;
        File file = new File(filePath);

        // Если файл не найден, пробуем добавить расширение .html
        if (!file.exists() && !path.contains(".")) {
            file = new File(filePath + ".html");
        }

        // Если файл все еще не найден, отдаем 404
        if (!file.exists() || file.isDirectory()) {
            sendNotFound(exchange);
            return;
        }

        // Определяем MIME-тип
        String mimeType = getMimeType(filePath);

        // Отправляем файл
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        String response = "<html><body><h1>404 Not Found</h1></body></html>";
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(404, response.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }
}