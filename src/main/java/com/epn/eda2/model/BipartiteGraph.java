package com.epn.eda2.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grafo Bipartito No Dirigido G = (U, V, E) donde:
 * - U es el conjunto de Usuarios.
 * - V es el conjunto de Películas (Ítems).
 * - E son las aristas (u, v) que representan interacciones positivas (rating >= threshold).
 *
 * Implementado mediante listas de adyacencia con Map<Integer, Set<Integer>>
 * garantizando acceso O(1) promedio tanto para consultas de vecindad como para
 * verificación de existencia de aristas.
 */
public class BipartiteGraph {
    public static final double DEFAULT_POSITIVE_RATING_THRESHOLD = 4.0;

    private final Map<Integer, Set<Integer>> userToMovies;
    private final Map<Integer, Set<Integer>> movieToUsers;
    private final Map<Integer, Movie> movieCatalog;
    private final Map<Long, Double> edgeRatings;
    private int edgeCount;

    public BipartiteGraph() {
        this.userToMovies = new HashMap<>();
        this.movieToUsers = new HashMap<>();
        this.movieCatalog = new HashMap<>();
        this.edgeRatings = new HashMap<>();
        this.edgeCount = 0;
    }

    /**
     * Registra una película en el catálogo V. Operación O(1).
     */
    public void addMovie(Movie movie) {
        if (movie != null) {
            movieCatalog.put(movie.getMovieId(), movie);
            movieToUsers.computeIfAbsent(movie.getMovieId(), k -> new HashSet<>());
        }
    }

    /**
     * Registra un usuario en el conjunto U. Operación O(1).
     */
    public void addUser(int userId) {
        userToMovies.computeIfAbsent(userId, k -> new HashSet<>());
    }

    /**
     * Agrega una arista no dirigida entre el usuario u y la película v en tiempo O(1).
     */
    public void addInteraction(int userId, int movieId, double rating) {
        Set<Integer> moviesOfUser = userToMovies.computeIfAbsent(userId, k -> new HashSet<>());
        Set<Integer> usersOfMovie = movieToUsers.computeIfAbsent(movieId, k -> new HashSet<>());

        if (!movieCatalog.containsKey(movieId)) {
            movieCatalog.put(movieId, new Movie(movieId, "Película #" + movieId, "Desconocido"));
        }

        if (moviesOfUser.add(movieId)) {
            usersOfMovie.add(userId);
            edgeCount++;
        }
        edgeRatings.put(encodeEdgeKey(userId, movieId), rating);
    }

    /**
     * Construye el grafo filtrando únicamente interacciones positivas (rating >= minRating).
     */
    public void buildFromDataset(Collection<Movie> movies, List<Rating> ratings, double minRating) {
        clear();
        if (movies != null) {
            for (Movie movie : movies) {
                addMovie(movie);
            }
        }
        if (ratings != null) {
            for (Rating r : ratings) {
                if (r.getRating() >= minRating) {
                    addInteraction(r.getUserId(), r.getMovieId(), r.getRating());
                }
            }
        }
    }

    /**
     * Limpia todas las estructuras del grafo.
     */
    public void clear() {
        userToMovies.clear();
        movieToUsers.clear();
        movieCatalog.clear();
        edgeRatings.clear();
        edgeCount = 0;
    }

    /**
     * Obtiene las películas vistas por un usuario u (Nivel 1 desde u) en tiempo O(1).
     */
    public Set<Integer> getMoviesWatchedByUser(int userId) {
        Set<Integer> movies = userToMovies.get(userId);
        return movies != null ? Collections.unmodifiableSet(movies) : Collections.emptySet();
    }

    /**
     * Obtiene los usuarios que vieron una película v (Nivel 2 desde p1) en tiempo O(1).
     */
    public Set<Integer> getUsersWhoWatchedMovie(int movieId) {
        Set<Integer> users = movieToUsers.get(movieId);
        return users != null ? Collections.unmodifiableSet(users) : Collections.emptySet();
    }

    /**
     * Verifica en O(1) si el usuario u ya ha visto la película v.
     */
    public boolean hasUserWatchedMovie(int userId, int movieId) {
        Set<Integer> movies = userToMovies.get(userId);
        return movies != null && movies.contains(movieId);
    }

    /**
     * Obtiene la calificación asociada a la arista (u, v) en O(1).
     */
    public double getRating(int userId, int movieId) {
        return edgeRatings.getOrDefault(encodeEdgeKey(userId, movieId), 0.0);
    }

    /**
     * Obtiene el objeto Movie en O(1).
     */
    public Movie getMovie(int movieId) {
        return movieCatalog.getOrDefault(movieId, new Movie(movieId, "Película #" + movieId, "Sin género"));
    }

    /**
     * Devuelve el conjunto ordenado de IDs de usuarios activos en el grafo.
     */
    public Set<Integer> getAllUserIds() {
        return new TreeSet<>(userToMovies.keySet());
    }

    /**
     * Devuelve el mapa interno de adyacencia Usuario -> Películas.
     */
    public Map<Integer, Set<Integer>> getUserToMovies() {
        return Collections.unmodifiableMap(userToMovies);
    }

    /**
     * Devuelve el mapa interno de adyacencia Película -> Usuarios.
     */
    public Map<Integer, Set<Integer>> getMovieToUsers() {
        return Collections.unmodifiableMap(movieToUsers);
    }

    public int getUserCount() {
        return userToMovies.size();
    }

    public int getMovieCount() {
        return movieCatalog.size();
    }

    public int getActiveMovieCount() {
        int active = 0;
        for (Set<Integer> users : movieToUsers.values()) {
            if (!users.isEmpty()) {
                active++;
            }
        }
        return active;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    private static long encodeEdgeKey(int userId, int movieId) {
        return (((long) userId) << 32) | (movieId & 0xFFFFFFFFL);
    }
}
