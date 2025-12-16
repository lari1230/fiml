package main.models;

import java.sql.Timestamp;

public class Review {
    private int id;
    private int movieId;
    private int userId;
    private String username;
    private String movieTitle;
    private int rating;
    private String comment;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isApproved;

    // Constructors
    public Review() {}

    public Review(int movieId, int userId, int rating, String comment) {
        this.movieId = movieId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.isApproved = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }
}