package com.epn.eda2.model;

/**
 * Representa una calificación explícita de un usuario hacia una película.
 */
public class Rating {
    private final int userId;
    private final int movieId;
    private final double rating;

    public Rating(int userId, int movieId, double rating) {
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
    }

    public int getUserId() {
        return userId;
    }

    public int getMovieId() {
        return movieId;
    }

    public double getRating() {
        return rating;
    }
}
