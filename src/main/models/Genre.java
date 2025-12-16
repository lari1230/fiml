package main.models;

public class Genre {
    private int id;
    private String name;
    private int movieCount;

    // Constructors
    public Genre() {}

    public Genre(String name) {
        this.name = name;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMovieCount() { return movieCount; }
    public void setMovieCount(int movieCount) { this.movieCount = movieCount; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}