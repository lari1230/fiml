package main.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 24 * 60 * 60 * 1000; // 24 часа
    private static final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    static {
        // Очистка устаревших сессий каждый час
        cleaner.scheduleAtFixedRate(SessionManager::cleanExpiredSessions,
                1, 1, TimeUnit.HOURS);
    }

    private static class SessionData {
        int userId;
        String role;
        long expiryTime;

        SessionData(int userId, String role) {
            this.userId = userId;
            this.role = role;
            this.expiryTime = System.currentTimeMillis() + SESSION_TIMEOUT;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public static String createSession(int userId, String role) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new SessionData(userId, role));
        return sessionId;
    }

    public static Integer getUserId(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null || data.isExpired()) {
            sessions.remove(sessionId);
            return null;
        }
        // Обновляем время истечения при активности
        data.expiryTime = System.currentTimeMillis() + SESSION_TIMEOUT;
        return data.userId;
    }

    public static String getUserRole(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null || data.isExpired()) {
            sessions.remove(sessionId);
            return null;
        }
        data.expiryTime = System.currentTimeMillis() + SESSION_TIMEOUT;
        return data.role;
    }

    public static void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private static void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public static void shutdown() {
        cleaner.shutdown();
    }
}