package main.services;

import main.dao.AdminDAO;
import main.dao.MovieDAO;
import main.dao.ReviewDAO;
import main.dao.UserDAO;
import main.dao.GenreDAO;
import main.models.Movie;
import main.models.Review;
import main.models.Genre;
import main.models.User;
import main.utils.Validator;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AdminService {
    private final AdminDAO adminDAO = new AdminDAO();
    private final UserDAO userDAO = new UserDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final GenreDAO genreDAO = new GenreDAO();

    // === Управление пользователями ===

    public List<Map<String, Object>> getUsers(int page, int limit, String filter) throws SQLException {
        List<User> users;

        switch (filter) {
            case "active":
                // Только активные пользователи
                users = userDAO.getAllUsers().stream()
                        .filter(User::isActive)
                        .toList();
                break;
            case "inactive":
                // Только неактивные пользователи
                users = userDAO.getAllUsers().stream()
                        .filter(user -> !user.isActive())
                        .toList();
                break;
            case "admins":
                // Только администраторы
                users = userDAO.getAllUsers().stream()
                        .filter(user -> "ADMIN".equals(user.getRole()))
                        .toList();
                break;
            default:
                // Все пользователи
                users = userDAO.getAllUsers();
        }

        // Пагинация
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, users.size());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            User user = users.get(i);
            result.add(mapUserToResponse(user));
        }

        return result;
    }

    public int getUsersCount(String filter) throws SQLException {
        List<User> users = userDAO.getAllUsers();

        switch (filter) {
            case "active":
                return (int) users.stream().filter(User::isActive).count();
            case "inactive":
                return (int) users.stream().filter(user -> !user.isActive()).count();
            case "admins":
                return (int) users.stream().filter(user -> "ADMIN".equals(user.getRole())).count();
            default:
                return users.size();
        }
    }

    public List<Map<String, Object>> searchUsers(String query) throws SQLException {
        List<User> allUsers = userDAO.getAllUsers();
        String searchQuery = query.toLowerCase();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : allUsers) {
            if (user.getUsername().toLowerCase().contains(searchQuery) ||
                    user.getEmail().toLowerCase().contains(searchQuery)) {
                result.add(mapUserToResponse(user));
            }
        }

        return result;
    }

    public boolean updateUser(int userId, String username, String email, String role, boolean isActive)
            throws SQLException {

        User user = userDAO.getUserById(userId);
        if (user == null) {
            return false;
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(isActive);

        return userDAO.updateUser(user);
    }

    public boolean updateUserStatus(int userId, boolean isActive) throws SQLException {
        return adminDAO.updateUserStatus(userId, isActive);
    }

    public boolean updateUserRole(int userId, String role) throws SQLException {
        return adminDAO.updateUserRole(userId, role);
    }

    public boolean deleteUser(int userId) throws SQLException {
        return userDAO.deleteUser(userId);
    }

    // === Управление фильмами ===

    public List<Map<String, Object>> getMovies(int page, int limit, String sortBy, String order)
            throws SQLException {

        List<Movie> movies = movieDAO.getAllMovies(sortBy, order, null);

        // Пагинация
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, movies.size());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Movie movie = movies.get(i);
            result.add(mapMovieToResponse(movie));
        }

        return result;
    }

    public int getMoviesCount() throws SQLException {
        return movieDAO.getAllMovies(null, null, null).size();
    }

    public List<Map<String, Object>> searchMovies(String query) throws SQLException {
        List<Movie> movies = movieDAO.searchMovies(query);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Movie movie : movies) {
            result.add(mapMovieToResponse(movie));
        }

        return result;
    }

    public Movie createMovie(String title, String director, Integer year,
                             String description, Integer duration, String posterUrl,
                             List<String> genres) throws SQLException {

        if (!Validator.isValidMovieTitle(title)) {
            throw new IllegalArgumentException("Invalid movie title");
        }

        if (year != null && !Validator.isValidYear(year)) {
            throw new IllegalArgumentException("Invalid year");
        }

        Movie movie = new Movie(title, director, year, description, duration);
        movie.setPosterUrl(posterUrl);

        Movie createdMovie = movieDAO.createMovie(movie);

        // Добавляем жанры
        if (genres != null && !genres.isEmpty()) {
            for (String genreName : genres) {
                Genre genre = getOrCreateGenre(genreName);
                if (genre != null) {
                    genreDAO.addGenreToMovie(createdMovie.getId(), genre.getId());
                }
            }
        }

        return createdMovie;
    }

    public boolean updateMovie(int movieId, String title, String director, Integer year,
                               String description, Integer duration, String posterUrl)
            throws SQLException {

        Movie movie = movieDAO.getMovieById(movieId);
        if (movie == null) {
            return false;
        }

        movie.setTitle(title);
        movie.setDirector(director);
        if (year != null) movie.setYear(year);
        movie.setDescription(description);
        if (duration != null) movie.setDuration(duration);
        movie.setPosterUrl(posterUrl);

        return movieDAO.updateMovie(movie);
    }

    public boolean deleteMovie(int movieId) throws SQLException {
        return movieDAO.deleteMovie(movieId);
    }

    // === Управление отзывами ===

    public List<Map<String, Object>> getReviews(int page, int limit, String filter) throws SQLException {
        List<Review> reviews;

        switch (filter) {
            case "pending":
                reviews = reviewDAO.getAllReviews(true);
                break;
            case "approved":
                reviews = reviewDAO.getAllReviews(false);
                break;
            default:
                reviews = reviewDAO.getAllReviews(false);
                reviews.addAll(reviewDAO.getAllReviews(true));
        }

        // Пагинация
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, reviews.size());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Review review = reviews.get(i);
            result.add(mapReviewToResponse(review));
        }

        return result;
    }

    public int getReviewsCount(String filter) throws SQLException {
        switch (filter) {
            case "pending":
                return reviewDAO.getAllReviews(true).size();
            case "approved":
                return reviewDAO.getAllReviews(false).size();
            default:
                return reviewDAO.getAllReviews(false).size() + reviewDAO.getAllReviews(true).size();
        }
    }

    public List<Map<String, Object>> getPendingReviews() throws SQLException {
        List<Review> reviews = reviewDAO.getAllReviews(true);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Review review : reviews) {
            result.add(mapReviewToResponse(review));
        }

        return result;
    }

    public List<Map<String, Object>> getReportedReviews() throws SQLException {
        // В реальном приложении здесь была бы логика для получения жалоб
        // Пока возвращаем пустой список
        return new ArrayList<>();
    }

    public boolean deleteReview(int reviewId) throws SQLException {
        return reviewDAO.deleteReview(reviewId);
    }

    public boolean approveReview(int reviewId) throws SQLException {
        return reviewDAO.approveReview(reviewId);
    }

    public boolean rejectReview(int reviewId) throws SQLException {
        return reviewDAO.rejectReview(reviewId);
    }

    // === Управление жанрами ===

    public List<Map<String, Object>> getGenresWithStats() throws SQLException {
        List<Genre> genres = genreDAO.getAllGenres();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Genre genre : genres) {
            Map<String, Object> genreData = new HashMap<>();
            genreData.put("id", genre.getId());
            genreData.put("name", genre.getName());

            // Получаем количество фильмов для жанра
            List<Movie> movies = movieDAO.getAllMovies(null, null, null);
            int movieCount = 0;
            for (Movie movie : movies) {
                List<Genre> movieGenres = genreDAO.getMovieGenres(movie.getId());
                if (movieGenres.stream().anyMatch(g -> g.getId() == genre.getId())) {
                    movieCount++;
                }
            }
            genreData.put("movieCount", movieCount);

            result.add(genreData);
        }

        return result;
    }

    public Genre createGenre(String name) throws SQLException {
        return genreDAO.createGenre(name);
    }

    public boolean updateGenre(int genreId, String name) throws SQLException {
        return adminDAO.updateGenre(genreId, name);
    }

    public boolean deleteGenre(int genreId) throws SQLException {
        return adminDAO.deleteGenre(genreId);
    }

    // === Статистика и дашборд ===

    public Map<String, Object> getDashboardStats() throws SQLException {
        AdminDAO.DashboardStats stats = adminDAO.getDashboardStats();

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", stats.getTotalUsers());
        result.put("totalMovies", stats.getTotalMovies());
        result.put("totalReviews", stats.getTotalReviews());
        result.put("pendingReviews", stats.getPendingReviews());
        result.put("averageRating", Math.round(stats.getAverageRating() * 10.0) / 10.0);
        result.put("activeUsers", stats.getActiveUsers());
        result.put("todayUsers", stats.getTodayUsers());
        result.put("todayReviews", stats.getTodayReviews());
        result.put("todayMovies", stats.getTodayMovies());

        // Топ фильмов
        List<AdminDAO.TopMovie> topMovies = adminDAO.getTopMoviesByRating(5);
        List<Map<String, Object>> topMoviesData = new ArrayList<>();
        for (AdminDAO.TopMovie movie : topMovies) {
            Map<String, Object> movieData = new HashMap<>();
            movieData.put("id", movie.getId());
            movieData.put("title", movie.getTitle());
            movieData.put("year", movie.getYear());
            movieData.put("director", movie.getDirector());
            movieData.put("averageRating", Math.round(movie.getAverageRating() * 10.0) / 10.0);
            movieData.put("reviewCount", movie.getReviewCount());
            topMoviesData.add(movieData);
        }
        result.put("topMovies", topMoviesData);

        return result;
    }

    public List<Map<String, Object>> getMonthlyStats(int year) throws SQLException {
        List<AdminDAO.MonthlyStats> stats = adminDAO.getMonthlyStats(year);

        // Группируем по месяцам
        Map<Integer, Map<String, Object>> monthlyData = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("users", 0);
            monthData.put("movies", 0);
            monthData.put("reviews", 0);
            monthlyData.put(month, monthData);
        }

        // Заполняем данные
        for (AdminDAO.MonthlyStats stat : stats) {
            Map<String, Object> monthData = monthlyData.get(stat.getMonth());
            if (monthData != null) {
                monthData.put(stat.getType(), stat.getCount());
            }
        }

        return new ArrayList<>(monthlyData.values());
    }

    public List<Map<String, Object>> getRecentActivity(int limit) throws SQLException {
        List<AdminDAO.RecentActivity> activities = adminDAO.getRecentActivity(limit);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminDAO.RecentActivity activity : activities) {
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("type", activity.getType());
            activityData.put("username", activity.getUsername());
            activityData.put("activityDate", activity.getActivityDate().toString());
            activityData.put("movieTitle", activity.getMovieTitle());
            activityData.put("rating", activity.getRating());
            activityData.put("comment", activity.getComment());
            result.add(activityData);
        }

        return result;
    }

    // === Системная информация ===

    public Map<String, Object> getSystemInfo() throws SQLException {
        AdminDAO.SystemInfo info = adminDAO.getSystemInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("dbVersion", info.getDbVersion());
        result.put("dbName", info.getDbName());
        result.put("dbSizeMB", info.getDbSizeMB());
        result.put("serverTime", info.getServerTime().toString());
        result.put("javaVersion", info.getJavaVersion());
        result.put("osName", info.getOsName());
        result.put("osVersion", info.getOsVersion());

        // Информация о памяти
        Runtime runtime = Runtime.getRuntime();
        result.put("totalMemory", runtime.totalMemory() / 1024 / 1024);
        result.put("freeMemory", runtime.freeMemory() / 1024 / 1024);
        result.put("maxMemory", runtime.maxMemory() / 1024 / 1024);
        result.put("availableProcessors", runtime.availableProcessors());

        return result;
    }

    public boolean createBackup(String backupPath) throws SQLException {
        return adminDAO.backupDatabase(backupPath);
    }

    // === Вспомогательные методы ===

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("isActive", user.isActive());
        response.put("createdAt", user.getCreatedAt().toString());

        try {
            // Получаем статистику пользователя
            List<Review> reviews = reviewDAO.getUserReviews(user.getId());
            response.put("reviewCount", reviews.size());

            if (!reviews.isEmpty()) {
                response.put("lastReviewDate", reviews.get(0).getCreatedAt().toString());
            }
        } catch (SQLException e) {
            response.put("reviewCount", 0);
        }

        return response;
    }

    private Map<String, Object> mapMovieToResponse(Movie movie) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", movie.getId());
        response.put("title", movie.getTitle());
        response.put("director", movie.getDirector());
        response.put("year", movie.getYear());
        response.put("description", movie.getDescription());
        response.put("duration", movie.getDuration());
        response.put("posterUrl", movie.getPosterUrl());
        response.put("averageRating", movie.getAverageRating());

        try {
            // Получаем количество отзывов
            response.put("reviewCount", reviewDAO.getReviewCount(movie.getId()));

            // Получаем жанры
            List<Genre> genres = genreDAO.getMovieGenres(movie.getId());
            List<Map<String, Object>> genreData = new ArrayList<>();
            for (Genre genre : genres) {
                Map<String, Object> genreInfo = new HashMap<>();
                genreInfo.put("id", genre.getId());
                genreInfo.put("name", genre.getName());
                genreData.add(genreInfo);
            }
            response.put("genres", genreData);
        } catch (SQLException e) {
            response.put("reviewCount", 0);
            response.put("genres", new ArrayList<>());
        }

        return response;
    }

    private Map<String, Object> mapReviewToResponse(Review review) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", review.getId());
        response.put("movieId", review.getMovieId());
        response.put("userId", review.getUserId());
        response.put("username", review.getUsername());
        response.put("movieTitle", review.getMovieTitle());
        response.put("rating", review.getRating());
        response.put("comment", review.getComment());
        response.put("createdAt", review.getCreatedAt().toString());
        response.put("updatedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null);
        response.put("isApproved", review.isApproved());

        return response;
    }

    private Genre getOrCreateGenre(String genreName) throws SQLException {
        List<Genre> allGenres = genreDAO.getAllGenres();

        // Ищем существующий жанр
        for (Genre genre : allGenres) {
            if (genre.getName().equalsIgnoreCase(genreName)) {
                return genre;
            }
        }

        // Создаем новый жанр
        return genreDAO.createGenre(genreName);
    }
}