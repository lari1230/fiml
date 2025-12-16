package main.dao;

import main.models.User;
import main.models.Movie;
import main.models.Review;
import main.models.Genre;
import main.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class AdminDAO {

    // === Управление пользователями ===

    public List<User> getAllUsersWithStats() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = """
            SELECT u.*, 
                   COUNT(r.id) as review_count,
                   MAX(r.created_at) as last_review_date
            FROM users u
            LEFT JOIN reviews r ON u.id = r.user_id
            GROUP BY u.id
            ORDER BY u.created_at DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                // Дополнительная статистика
                user.setReviewCount(rs.getInt("review_count"));
                user.setLastReviewDate(rs.getTimestamp("last_review_date"));
                users.add(user);
            }
        }
        return users;
    }

    public boolean updateUserStatus(int userId, boolean isActive) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateUserRole(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    // === Статистика ===

    public DashboardStats getDashboardStats() throws SQLException {
        DashboardStats stats = new DashboardStats();

        // Общая статистика
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM users) as total_users,
                (SELECT COUNT(*) FROM movies) as total_movies,
                (SELECT COUNT(*) FROM reviews) as total_reviews,
                (SELECT COUNT(*) FROM reviews WHERE is_approved = FALSE) as pending_reviews,
                (SELECT AVG(rating) FROM reviews WHERE is_approved = TRUE) as avg_rating,
                (SELECT COUNT(DISTINCT user_id) FROM reviews) as active_users
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setTotalUsers(rs.getInt("total_users"));
                stats.setTotalMovies(rs.getInt("total_movies"));
                stats.setTotalReviews(rs.getInt("total_reviews"));
                stats.setPendingReviews(rs.getInt("pending_reviews"));
                stats.setAverageRating(rs.getDouble("avg_rating"));
                stats.setActiveUsers(rs.getInt("active_users"));
            }
        }

        // Статистика за сегодня
        String todaySql = """
            SELECT 
                (SELECT COUNT(*) FROM users WHERE DATE(created_at) = CURDATE()) as today_users,
                (SELECT COUNT(*) FROM reviews WHERE DATE(created_at) = CURDATE()) as today_reviews,
                (SELECT COUNT(*) FROM movies WHERE DATE(created_at) = CURDATE()) as today_movies
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(todaySql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setTodayUsers(rs.getInt("today_users"));
                stats.setTodayReviews(rs.getInt("today_reviews"));
                stats.setTodayMovies(rs.getInt("today_movies"));
            }
        }

        return stats;
    }

    public List<MonthlyStats> getMonthlyStats(int year) throws SQLException {
        List<MonthlyStats> stats = new ArrayList<>();
        String sql = """
            SELECT 
                MONTH(created_at) as month,
                COUNT(*) as count,
                'users' as type
            FROM users
            WHERE YEAR(created_at) = ?
            GROUP BY MONTH(created_at)
            
            UNION ALL
            
            SELECT 
                MONTH(created_at) as month,
                COUNT(*) as count,
                'reviews' as type
            FROM reviews
            WHERE YEAR(created_at) = ?
            GROUP BY MONTH(created_at)
            
            UNION ALL
            
            SELECT 
                MONTH(created_at) as month,
                COUNT(*) as count,
                'movies' as type
            FROM movies
            WHERE YEAR(created_at) = ?
            GROUP BY MONTH(created_at)
            
            ORDER BY month, type
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, year);
            stmt.setInt(3, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MonthlyStats monthlyStat = new MonthlyStats();
                    monthlyStat.setMonth(rs.getInt("month"));
                    monthlyStat.setCount(rs.getInt("count"));
                    monthlyStat.setType(rs.getString("type"));
                    stats.add(monthlyStat);
                }
            }
        }
        return stats;
    }

    public List<TopMovie> getTopMoviesByRating(int limit) throws SQLException {
        List<TopMovie> topMovies = new ArrayList<>();
        String sql = """
            SELECT 
                m.id,
                m.title,
                m.year,
                m.director,
                AVG(r.rating) as avg_rating,
                COUNT(r.id) as review_count
            FROM movies m
            LEFT JOIN reviews r ON m.id = r.movie_id AND r.is_approved = TRUE
            GROUP BY m.id
            HAVING avg_rating IS NOT NULL
            ORDER BY avg_rating DESC, review_count DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TopMovie movie = new TopMovie();
                    movie.setId(rs.getInt("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setAverageRating(rs.getDouble("avg_rating"));
                    movie.setReviewCount(rs.getInt("review_count"));
                    topMovies.add(movie);
                }
            }
        }
        return topMovies;
    }

    public List<RecentActivity> getRecentActivity(int limit) throws SQLException {
        List<RecentActivity> activities = new ArrayList<>();

        String sql = """
            SELECT 
                'user_registered' as type,
                u.username,
                u.created_at as activity_date,
                NULL as movie_title,
                NULL as rating,
                NULL as comment
            FROM users u
            WHERE u.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
            
            UNION ALL
            
            SELECT 
                'review_added' as type,
                u.username,
                r.created_at as activity_date,
                m.title as movie_title,
                r.rating,
                r.comment
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            WHERE r.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
            
            UNION ALL
            
            SELECT 
                'movie_added' as type,
                'admin' as username,
                m.created_at as activity_date,
                m.title as movie_title,
                NULL as rating,
                NULL as comment
            FROM movies m
            WHERE m.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
            
            ORDER BY activity_date DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecentActivity activity = new RecentActivity();
                    activity.setType(rs.getString("type"));
                    activity.setUsername(rs.getString("username"));
                    activity.setActivityDate(rs.getTimestamp("activity_date"));
                    activity.setMovieTitle(rs.getString("movie_title"));
                    activity.setRating(rs.getInt("rating"));
                    activity.setComment(rs.getString("comment"));
                    activities.add(activity);
                }
            }
        }
        return activities;
    }

    // === Управление жанрами ===

    public List<Genre> getAllGenresWithStats() throws SQLException {
        List<Genre> genres = new ArrayList<>();
        String sql = """
            SELECT 
                g.*,
                COUNT(mg.movie_id) as movie_count
            FROM genres g
            LEFT JOIN movie_genres mg ON g.id = mg.genre_id
            GROUP BY g.id
            ORDER BY movie_count DESC, g.name
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                genre.setMovieCount(rs.getInt("movie_count"));
                genres.add(genre);
            }
        }
        return genres;
    }

    public boolean deleteGenre(int genreId) throws SQLException {
        String sql = "DELETE FROM genres WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, genreId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateGenre(int genreId, String name) throws SQLException {
        String sql = "UPDATE genres SET name = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setInt(2, genreId);

            return stmt.executeUpdate() > 0;
        }
    }

    // === Системные операции ===

    public boolean backupDatabase(String backupPath) throws SQLException {
        // В реальном приложении здесь был бы код для создания бэкапа БД
        // Это упрощенная версия
        String sql = "SELECT * FROM users INTO OUTFILE ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, backupPath + "/users_backup.csv");
            stmt.execute();

            // Аналогично для других таблиц...
            return true;
        }
    }

    public SystemInfo getSystemInfo() throws SQLException {
        SystemInfo info = new SystemInfo();

        // Информация о базе данных
        String dbSql = "SELECT VERSION() as db_version, DATABASE() as db_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                info.setDbVersion(rs.getString("db_version"));
                info.setDbName(rs.getString("db_name"));
            }
        }

        // Размер базы данных
        String sizeSql = """
            SELECT 
                ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as db_size_mb
            FROM information_schema.TABLES
            WHERE table_schema = DATABASE()
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sizeSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                info.setDbSizeMB(rs.getDouble("db_size_mb"));
            }
        }

        info.setServerTime(new java.util.Date());
        info.setJavaVersion(System.getProperty("java.version"));
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));

        return info;
    }

    // === Вспомогательные методы ===

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    // === Внутренние классы для данных ===

    public static class DashboardStats {
        private int totalUsers;
        private int totalMovies;
        private int totalReviews;
        private int pendingReviews;
        private double averageRating;
        private int activeUsers;
        private int todayUsers;
        private int todayReviews;
        private int todayMovies;

        // Getters and Setters
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

        public int getTotalMovies() { return totalMovies; }
        public void setTotalMovies(int totalMovies) { this.totalMovies = totalMovies; }

        public int getTotalReviews() { return totalReviews; }
        public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

        public int getPendingReviews() { return pendingReviews; }
        public void setPendingReviews(int pendingReviews) { this.pendingReviews = pendingReviews; }

        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

        public int getActiveUsers() { return activeUsers; }
        public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

        public int getTodayUsers() { return todayUsers; }
        public void setTodayUsers(int todayUsers) { this.todayUsers = todayUsers; }

        public int getTodayReviews() { return todayReviews; }
        public void setTodayReviews(int todayReviews) { this.todayReviews = todayReviews; }

        public int getTodayMovies() { return todayMovies; }
        public void setTodayMovies(int todayMovies) { this.todayMovies = todayMovies; }
    }

    public static class MonthlyStats {
        private int month;
        private int count;
        private String type;

        // Getters and Setters
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class TopMovie {
        private int id;
        private String title;
        private int year;
        private String director;
        private double averageRating;
        private int reviewCount;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public String getDirector() { return director; }
        public void setDirector(String director) { this.director = director; }

        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

        public int getReviewCount() { return reviewCount; }
        public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    }

    public static class RecentActivity {
        private String type;
        private String username;
        private Timestamp activityDate;
        private String movieTitle;
        private Integer rating;
        private String comment;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Timestamp getActivityDate() { return activityDate; }
        public void setActivityDate(Timestamp activityDate) { this.activityDate = activityDate; }

        public String getMovieTitle() { return movieTitle; }
        public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class SystemInfo {
        private String dbVersion;
        private String dbName;
        private double dbSizeMB;
        private Date serverTime;
        private String javaVersion;
        private String osName;
        private String osVersion;

        // Getters and Setters
        public String getDbVersion() { return dbVersion; }
        public void setDbVersion(String dbVersion) { this.dbVersion = dbVersion; }

        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }

        public double getDbSizeMB() { return dbSizeMB; }
        public void setDbSizeMB(double dbSizeMB) { this.dbSizeMB = dbSizeMB; }

        public Date getServerTime() { return serverTime; }
        public void setServerTime(Date serverTime) { this.serverTime = serverTime; }

        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }

        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    }
}