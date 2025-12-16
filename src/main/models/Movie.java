package main.models;

import java.util.List;

public class Movie {
    private int id;
    private String title;
    private String director;
    private int year;
    private String description;
    private int duration;
    private String posterUrl;
    private double averageRating;
    private List<Genre> genres;
    private List<Review> reviews;

    // Constructors
    public Movie() {}

    public Movie(String title, String director, int year, String description, int duration) {
        this.title = title;
        this.director = director;
        this.year = year;
        this.description = description;
        this.duration = duration;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public List<Genre> getGenres() { return genres; }
    public void setGenres(List<Genre> genres) { this.genres = genres; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}