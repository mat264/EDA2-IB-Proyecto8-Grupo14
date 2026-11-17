package com.epn.eda2.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Representa un resultado de recomendación en Nivel 3 del recorrido BFS,
 * incluyendo el puntaje calculado (frecuencia de caminos + Jaccard) y su trazabilidad explicable.
 */
public class Recommendation implements Comparable<Recommendation> {
    private final Movie movie;
    private final double score;
    private final int pathCount;
    private final double jaccardAccumulated;
    private final Set<Integer> bridgeMoviesP1;
    private final Set<Integer> similarUsersUPrime;
    private final String explanation;

    public Recommendation(
            Movie movie,
            double score,
            int pathCount,
            double jaccardAccumulated,
            Set<Integer> bridgeMoviesP1,
            Set<Integer> similarUsersUPrime,
            String explanation) {
        this.movie = movie;
        this.score = score;
        this.pathCount = pathCount;
        this.jaccardAccumulated = jaccardAccumulated;
        this.bridgeMoviesP1 = new LinkedHashSet<>(bridgeMoviesP1);
        this.similarUsersUPrime = new LinkedHashSet<>(similarUsersUPrime);
        this.explanation = explanation;
    }

    public Movie getMovie() {
        return movie;
    }

    public int getMovieId() {
        return movie.getMovieId();
    }

    public String getTitle() {
        return movie.getTitle();
    }

    public String getGenres() {
        return movie.getGenres();
    }

    public double getScore() {
        return score;
    }

    public int getPathCount() {
        return pathCount;
    }

    public double getJaccardAccumulated() {
        return jaccardAccumulated;
    }

    public Set<Integer> getBridgeMoviesP1() {
        return Collections.unmodifiableSet(bridgeMoviesP1);
    }

    public Set<Integer> getSimilarUsersUPrime() {
        return Collections.unmodifiableSet(similarUsersUPrime);
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public int compareTo(Recommendation other) {
        int cmp = Double.compare(other.score, this.score);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(other.pathCount, this.pathCount);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(this.movie.getMovieId(), other.movie.getMovieId());
    }
}
