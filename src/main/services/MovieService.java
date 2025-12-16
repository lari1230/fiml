package main.services;

import main.dao.MovieDAO;
import main.dao.GenreDAO;
import main.dao.ReviewDAO;
import main.models.Movie;
import main.models.Review;
import main.models.Genre;
import main.utils.Validator;
import java.sql.SQLException;
import java.util.List;

public class MovieService {
    private final MovieDAO movieDAO = new MovieDAO();
    private final GenreDAO genreDAO = new GenreDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    public Movie createMovie(String title, String director, int year,
                             String description, int duration, String posterUrl)
            throws SQLException {

        if (!Validator.isValidMovieTitle(title)) {
            throw new IllegalArgumentException("Некорректное название фильма");
        }

        if (!Validator.isValidYear(year)) {
            throw new IllegalArgumentException("Некорректный год выпуска");
        }

        if (duration <= 0 || duration > 600) {
            throw new IllegalArgumentException("Некорректная продолжительность фильма");
        }

        Movie movie = new Movie(title, director, year, description, duration);
        movie.setPosterUrl(posterUrl);

        return movieDAO.createMovie(movie);
    }

    public Movie getMovieById(int id) throws SQLException {
        Movie movie = movieDAO.getMovieById(id);

        if (movie != null) {
            // Загружаем жанры
            List<Genre> genres = genreDAO.getMovieGenres(id);
            movie.setGenres(genres);

            // Загружаем отзывы
            List<Review> reviews = reviewDAO.getMovieReviews(id, false);
            movie.setReviews(reviews);
        }

        return movie;
    }

    public List<Movie> getAllMovies(String sortBy, String order, Integer limit) throws SQLException {
        return movieDAO.getAllMovies(sortBy, order, limit);
    }

    public List<Movie> searchMovies(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllMovies(null, null, 50);
        }

        return movieDAO.searchMovies(query.trim());
    }

    public List<Movie> getTopRatedMovies(int limit) throws SQLException {
        return movieDAO.getTopRatedMovies(limit);
    }

    public List<Movie> getMoviesByYearRange(int fromYear, int toYear) throws SQLException {
        return movieDAO.getMoviesByYearRange(fromYear, toYear);
    }

    public boolean updateMovie(int movieId, String title, String director, int year,
                               String description, int duration, String posterUrl)
            throws SQLException {

        Movie movie = movieDAO.getMovieById(movieId);
        if (movie == null) {
            return false;
        }

        movie.setTitle(title);
        movie.setDirector(director);
        movie.setYear(year);
        movie.setDescription(description);
        movie.setDuration(duration);
        movie.setPosterUrl(posterUrl);

        return movieDAO.updateMovie(movie);
    }

    public boolean deleteMovie(int movieId) throws SQLException {
        return movieDAO.deleteMovie(movieId);
    }

    public boolean addGenresToMovie(int movieId, List<String> genreNames) throws SQLException {
        List<Genre> genres = genreDAO.getGenresByNames(genreNames);

        boolean success = true;
        for (Genre genre : genres) {
            if (!genreDAO.addGenreToMovie(movieId, genre.getId())) {
                success = false;
            }
        }

        return success;
    }

    public boolean removeGenreFromMovie(int movieId, int genreId) throws SQLException {
        return genreDAO.removeGenreFromMovie(movieId, genreId);
    }
}