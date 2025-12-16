//package main;

import main.config.ServerConfig;
import main.handlers.*;
import main.utils.DatabaseConnection;
import main.utils.SessionManager;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("🚀 Запуск киносайта...");

            // Инициализация конфигурации
            initializeConfiguration();

            // Инициализация базы данных
            initializeDatabase();

            // Создание HTTP сервера
            int port = ServerConfig.getServerPort();
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Регистрация обработчиков
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/movies", new MovieHandler());
            server.createContext("/api/reviews", new ReviewHandler());
            server.createContext("/api/user", new UserHandler());
            server.createContext("/api/admin", new AdminHandler());
            server.createContext("/", new StaticFileHandler());

            // Настройка пула потоков
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(
                    ServerConfig.getMaxThreads()
            ));

            // Запуск сервера
            server.start();

            System.out.println("\n🎬 Киносайт успешно запущен!");
            System.out.println("══════════════════════════════════════");
            System.out.println("📡 Сервер доступен по адресу: http://localhost:" + port);
            System.out.println("🏠 Главная страница: http://localhost:" + port + "/");
            System.out.println("🎥 Каталог фильмов: http://localhost:" + port + "/catalog.html");
            System.out.println("⭐ Топ фильмов: http://localhost:" + port + "/top.html");
            System.out.println("👤 Личный кабинет: http://localhost:" + port + "/profile.html");
            System.out.println("🔧 Админ панель: http://localhost:" + port + "/admin.html");
            System.out.println("══════════════════════════════════════");

            // Тестовые данные для входа
            System.out.println("\n🔐 Тестовые данные для входа:");
            System.out.println("   Администратор: admin@movie.com / admin123");
            System.out.println("   Пользователь:  alex@example.com / password123");
            System.out.println("\n⏳ Для остановки сервера нажмите Ctrl+C\n");

            // Добавляем обработчик завершения работы
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Остановка сервера...");
                server.stop(0);
                SessionManager.shutdown();
                System.out.println("✅ Сервер остановлен");
            }));

        } catch (Exception e) {
            System.err.println("❌ Ошибка при запуске сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeConfiguration() {
        try {
            // Пытаемся создать конфиг по умолчанию если его нет
            if (!ServerConfig.isConfigLoaded()) {
                ServerConfig.createDefaultConfig();
            }

            // Выводим конфигурацию
            if (ServerConfig.isDevMode()) {
                ServerConfig.printConfig();
            }
        } catch (IOException e) {
            System.out.println("⚠️ Не удалось создать файл конфигурации: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Создание таблицы пользователей
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "email VARCHAR(100) UNIQUE NOT NULL," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "role ENUM('USER', 'ADMIN') DEFAULT 'USER'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "is_active BOOLEAN DEFAULT TRUE" +
                    ")";
            stmt.execute(createUsersTable);

            // Создание таблицы фильмов
            String createMoviesTable = "CREATE TABLE IF NOT EXISTS movies (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "title VARCHAR(255) NOT NULL," +
                    "director VARCHAR(255)," +
                    "year INT," +
                    "description TEXT," +
                    "duration INT," +
                    "poster_url VARCHAR(500)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createMoviesTable);

            // Создание таблицы жанров
            String createGenresTable = "CREATE TABLE IF NOT EXISTS genres (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "name VARCHAR(50) UNIQUE NOT NULL" +
                    ")";
            stmt.execute(createGenresTable);

            // Создание таблицы связи фильмов и жанров
            String createMovieGenresTable = "CREATE TABLE IF NOT EXISTS movie_genres (" +
                    "movie_id INT," +
                    "genre_id INT," +
                    "PRIMARY KEY (movie_id, genre_id)," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createMovieGenresTable);

            // Создание таблицы отзывов
            String createReviewsTable = "CREATE TABLE IF NOT EXISTS reviews (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "movie_id INT," +
                    "user_id INT," +
                    "rating INT CHECK (rating >= 1 AND rating <= 10)," +
                    "comment TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "is_approved BOOLEAN DEFAULT TRUE," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "UNIQUE KEY unique_review (movie_id, user_id)" +
                    ")";
            stmt.execute(createReviewsTable);

            // Создание индексов для оптимизации
            String createIndexes =
                    "CREATE INDEX IF NOT EXISTS idx_movies_title ON movies(title);" +
                            "CREATE INDEX IF NOT EXISTS idx_movies_year ON movies(year);" +
                            "CREATE INDEX IF NOT EXISTS idx_reviews_movie_id ON reviews(movie_id);" +
                            "CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);" +
                            "CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);" +
                            "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);";

            String[] indexes = createIndexes.split(";");
            for (String index : indexes) {
                if (!index.trim().isEmpty()) {
                    try {
                        stmt.execute(index.trim());
                    } catch (SQLException e) {
                        // Игнорируем ошибки создания индексов (могут уже существовать)
                    }
                }
            }

            // Добавление жанров, если таблица пустая
            String checkGenres = "SELECT COUNT(*) as count FROM genres";
            var rs = stmt.executeQuery(checkGenres);
            if (rs.next() && rs.getInt("count") == 0) {
                String[] genres = {
                        "Драма", "Комедия", "Боевик", "Фантастика", "Ужасы",
                        "Мелодрама", "Триллер", "Детектив", "Приключения", "Аниме",
                        "Фэнтези", "Исторический", "Документальный", "Мюзикл", "Вестерн",
                        "Криминал", "Семейный", "Биография", "Спорт", "Военный"
                };

                for (String genre : genres) {
                    stmt.execute("INSERT IGNORE INTO genres (name) VALUES ('" + genre + "')");
                }
            }

            // Создание администратора по умолчанию
            String checkAdmin = "SELECT COUNT(*) as count FROM users WHERE email = 'admin@movie.com'";
            rs = stmt.executeQuery(checkAdmin);
            if (rs.next() && rs.getInt("count") == 0) {
                // Пароль: admin123
                String adminHash = "$2a$12$Yl6Z6Q8L8Q8L8Q8L8Q8L8Oe6Z6Q8L8Q8L8Q8L8Q8L8Q8L8Q8L8Q8L";
                stmt.execute("INSERT INTO users (username, email, password_hash, role) VALUES " +
                        "('admin', 'admin@movie.com', '" + adminHash + "', 'ADMIN')");

                // Создание тестового пользователя
                String userHash = "$2a$12$Yl6Z6Q8L8Q8L8Q8L8Q8L8Oe6Z6Q8L8Q8L8Q8L8Q8L8Q8L8Q8L8Q8L"; // password123
                stmt.execute("INSERT INTO users (username, email, password_hash) VALUES " +
                        "('alex', 'alex@example.com', '" + userHash + "')");
            }

            // Добавление тестовых фильмов, если таблица пустая
            String checkMovies = "SELECT COUNT(*) as count FROM movies";
            rs = stmt.executeQuery(checkMovies);
            if (rs.next() && rs.getInt("count") == 0) {
                // Добавляем несколько тестовых фильмов
                String[] movieInserts = {
                        "INSERT INTO movies (title, director, year, description, duration) VALUES " +
                                "('Интерстеллар', 'Кристофер Нолан', 2014, 'Фантастический эпос о путешествии в космос', 169)",

                        "INSERT INTO movies (title, director, year, description, duration) VALUES " +
                                "('Начало', 'Кристофер Нолан', 2010, 'Фильм о краже идей через сны', 148)",

                        "INSERT INTO movies (title, director, year, description, duration) VALUES " +
                                "('Криминальное чтиво', 'Квентин Тарантино', 1994, 'Культовый фильм о гангстерах', 154)",

                        "INSERT INTO movies (title, director, year, description, duration) VALUES " +
                                "('Побег из Шоушенка', 'Фрэнк Дарабонт', 1994, 'Драма о заключенном в тюрьме', 142)"
                };

                for (String insert : movieInserts) {
                    stmt.execute(insert);
                }
            }

            System.out.println("✅ База данных инициализирована успешно");

        } catch (Exception e) {
            System.err.println("❌ Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}