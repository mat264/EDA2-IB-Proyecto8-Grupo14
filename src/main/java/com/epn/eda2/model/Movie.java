package com.epn.eda2.model;

import java.util.Objects;

/**
 * Representa un nodo de tipo Película (Ítem v in V) dentro del grafo bipartito.
 */
public class Movie {
    private final int movieId;
    private final String title;
    private final String genres;

    public Movie(int movieId, String title, String genres) {
        this.movieId = movieId;
        this.title = title != null ? title.trim() : "Película #" + movieId;
        this.genres = genres != null ? genres.trim() : "Sin género";
    }

    public int getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getGenres() {
        return genres;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movie movie)) return false;
        return movieId == movie.movieId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId);
    }

    @Override
    public String toString() {
        return "[" + movieId + "] " + title + " (" + genres + ")";
    }
}
