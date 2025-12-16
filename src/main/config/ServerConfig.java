package main.config;

import main.utils.SessionManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    // Константы с значениями по умолчанию
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/movie_db";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "admin123";
    private static final int DEFAULT_MAX_THREADS = 10;
    private static final String DEFAULT_WEB_ROOT = "src/main/resources/web";
    private static final boolean DEFAULT_DEV_MODE = true;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            loaded = true;
            System.out.println("✅ Конфигурация загружена из config.properties");
        } catch (IOException e) {
            System.out.println("⚠️ Файл config.properties не найден. Используются значения по умолчанию.");
            setDefaultProperties();
        }
    }

    private static void setDefaultProperties() {
        properties.setProperty("server.port", String.valueOf(DEFAULT_PORT));
        properties.setProperty("db.url", DEFAULT_DB_URL);
        properties.setProperty("db.user", DEFAULT_DB_USER);
        properties.setProperty("db.password", DEFAULT_DB_PASSWORD);
        properties.setProperty("server.maxThreads", String.valueOf(DEFAULT_MAX_THREADS));
        properties.setProperty("server.webRoot", DEFAULT_WEB_ROOT);
        properties.setProperty("server.devMode", String.valueOf(DEFAULT_DEV_MODE));
        properties.setProperty("session.timeout", "86400"); // 24 часа в секундах
        properties.setProperty("cors.allowedOrigins", "*");
        properties.setProperty("upload.maxFileSize", "10485760"); // 10MB
        properties.setProperty("rate.limit.requests", "100");
        properties.setProperty("rate.limit.period", "3600"); // 1 час
    }

    // Методы для получения конфигурационных значений
    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
    }

    public static String getDatabaseUrl() {
        return properties.getProperty("db.url", DEFAULT_DB_URL);
    }

    public static String getDatabaseUser() {
        return properties.getProperty("db.user", DEFAULT_DB_USER);
    }

    public static String getDatabasePassword() {
        return properties.getProperty("db.password", DEFAULT_DB_PASSWORD);
    }

    public static int getMaxThreads() {
        return Integer.parseInt(properties.getProperty("server.maxThreads", String.valueOf(DEFAULT_MAX_THREADS)));
    }

    public static String getWebRoot() {
        return properties.getProperty("server.webRoot", DEFAULT_WEB_ROOT);
    }

    public static boolean isDevMode() {
        return Boolean.parseBoolean(properties.getProperty("server.devMode", String.valueOf(DEFAULT_DEV_MODE)));
    }

    public static long getSessionTimeout() {
        return Long.parseLong(properties.getProperty("session.timeout", "86400")) * 1000; // Конвертируем в миллисекунды
    }

    public static String getAllowedOrigins() {
        return properties.getProperty("cors.allowedOrigins", "*");
    }

    public static long getMaxFileSize() {
        return Long.parseLong(properties.getProperty("upload.maxFileSize", "10485760"));
    }

    public static int getRateLimitRequests() {
        return Integer.parseInt(properties.getProperty("rate.limit.requests", "100"));
    }

    public static int getRateLimitPeriod() {
        return Integer.parseInt(properties.getProperty("rate.limit.period", "3600"));
    }

    // Методы для обновления конфигурации
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Проверка загрузки конфигурации
    public static boolean isConfigLoaded() {
        return loaded;
    }

    // Метод для вывода всей конфигурации (для отладки)
    public static void printConfig() {
        System.out.println("\n📋 Конфигурация сервера:");
        System.out.println("══════════════════════════════════════");
        properties.forEach((key, value) -> {
            // Маскируем пароль при выводе
            if (key.toString().contains("password")) {
                System.out.printf("%-25s: %s%n", key, "******");
            } else {
                System.out.printf("%-25s: %s%n", key, value);
            }
        });
        System.out.println("══════════════════════════════════════\n");
    }

    // Метод для создания конфигурационного файла по умолчанию
    public static void createDefaultConfig() throws IOException {
        java.nio.file.Files.write(
                java.nio.file.Paths.get("config.properties"),
                getDefaultConfigContent().getBytes()
        );
        System.out.println("✅ Создан файл config.properties с настройками по умолчанию");
    }

    private static String getDefaultConfigContent() {
        return """
# Конфигурация сервера киносайта
# =================================

# Настройки сервера
server.port=8080
server.maxThreads=10
server.webRoot=src/main/resources/web
server.devMode=true

# Настройки базы данных
db.url=jdbc:mysql://localhost:3306/movie_db
db.user=root
db.password=password
db.poolSize=10
db.maxPoolSize=20
db.connectionTimeout=30000
db.idleTimeout=600000

# Настройки сессий
session.timeout=86400
session.cleanup.interval=3600

# Настройки безопасности
cors.allowedOrigins=*
upload.maxFileSize=10485760
password.minLength=8
password.requireUppercase=true
password.requireLowercase=true
password.requireNumbers=true

# Настройки ограничения запросов
rate.limit.requests=100
rate.limit.period=3600
rate.limit.ip.enabled=true

# Настройки логирования
log.level=INFO
log.file=logs/movie-site.log
log.maxSize=10485760
log.maxFiles=5

# Настройки почты (для восстановления пароля)
mail.enabled=false
mail.host=smtp.gmail.com
mail.port=587
mail.username=
mail.password=
mail.from=noreply@movie-site.com

# Настройки кэширования
cache.enabled=true
cache.ttl=3600
cache.maxSize=1000

# Настройки поиска
search.minQueryLength=2
search.maxResults=50
search.fuzzy.enabled=true
search.fuzzy.threshold=0.7
""";
    }
}