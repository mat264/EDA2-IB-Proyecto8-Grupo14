package com.epn.eda2.util;

import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Rating;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Cargador portable de datasets CSV (Películas y Calificaciones).
 * Soporta:
 * 1. Recursos embebidos en el JAR/classpath (src/main/resources/data/...) mediante getResourceAsStream().
 * 2. Rutas relativas locales (data/movies.csv, data/ratings.csv).
 * 3. Archivos externos seleccionados en vivo mediante FileChooser.
 */
public class CsvDatasetLoader {

    public static final String SYNTHETIC_MOVIES_RESOURCE = "/data/synthetic_movies.csv";
    public static final String SYNTHETIC_RATINGS_RESOURCE = "/data/synthetic_ratings.csv";

    public static final String MOVIELENS_MOVIES_RESOURCE = "/data/movies.csv";
    public static final String MOVIELENS_RATINGS_RESOURCE = "/data/ratings.csv";

    public static final String RELATIVE_MOVIES_PATH = "data/movies.csv";
    public static final String RELATIVE_RATINGS_PATH = "data/ratings.csv";

    /**
     * Carga películas desde una ruta relativa o desde el recurso embebido en classpath.
     */
    public static List<Movie> loadMoviesPortable(String relativeFilePath, String classpathResource) throws IOException {
        try (InputStream is = openPortableStream(relativeFilePath, classpathResource)) {
            return parseMoviesStream(is);
        }
    }

    /**
     * Carga calificaciones desde una ruta relativa o desde el recurso embebido en classpath.
     */
    public static List<Rating> loadRatingsPortable(String relativeFilePath, String classpathResource) throws IOException {
        try (InputStream is = openPortableStream(relativeFilePath, classpathResource)) {
            return parseRatingsStream(is);
        }
    }

    /**
     * Carga películas desde un archivo externo seleccionado por el usuario (FileChooser).
     */
    public static List<Movie> loadMoviesFromFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return parseMoviesStream(is);
        }
    }

    /**
     * Carga calificaciones desde un archivo externo seleccionado por el usuario (FileChooser).
     */
    public static List<Rating> loadRatingsFromFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return parseRatingsStream(is);
        }
    }

    /**
     * Intenta abrir primero una ruta relativa externa (si existe en el directorio de ejecución);
     * de lo contrario, utiliza getResourceAsStream() para leer el dataset embebido en resources.
     */
    private static InputStream openPortableStream(String relativeFilePath, String classpathResource) throws IOException {
        if (relativeFilePath != null && !relativeFilePath.isBlank()) {
            Path relPath = Paths.get(relativeFilePath);
            if (Files.exists(relPath) && Files.isRegularFile(relPath)) {
                return Files.newInputStream(relPath);
            }
        }
        String normalizedResource = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
        InputStream resourceStream = CsvDatasetLoader.class.getResourceAsStream(normalizedResource);
        if (resourceStream != null) {
            return resourceStream;
        }
        throw new IOException("No se encontró el recurso de datos ni en '"
                + relativeFilePath + "' ni en classpath '" + normalizedResource + "'.");
    }

    /**
     * Parsea un flujo CSV con formato: movieId,title,genres
     * Soporta títulos entre comillas que contengan comas.
     */
    public static List<Movie> parseMoviesStream(InputStream is) throws IOException {
        List<Movie> movies = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().startsWith("movieid") || line.toLowerCase().startsWith("\ufeffmovieid")) {
                        continue;
                    }
                }
                List<String> tokens = splitCsvLine(line);
                if (tokens.size() >= 2) {
                    try {
                        int movieId = Integer.parseInt(tokens.get(0).trim());
                        String title = tokens.get(1).trim();
                        String genres = tokens.size() >= 3 ? tokens.get(2).trim() : "General";
                        movies.add(new Movie(movieId, title, genres));
                    } catch (NumberFormatException ignored) {
                        // Ignorar filas malformadas
                    }
                }
            }
        }
        return movies;
    }

    /**
     * Parsea un flujo CSV con formato: userId,movieId,rating[,timestamp]
     */
    public static List<Rating> parseRatingsStream(InputStream is) throws IOException {
        List<Rating> ratings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().startsWith("userid") || line.toLowerCase().startsWith("\ufeffuserid")) {
                        continue;
                    }
                }
                List<String> tokens = splitCsvLine(line);
                if (tokens.size() >= 3) {
                    try {
                        int userId = Integer.parseInt(tokens.get(0).trim());
                        int movieId = Integer.parseInt(tokens.get(1).trim());
                        double rating = Double.parseDouble(tokens.get(2).trim());
                        ratings.add(new Rating(userId, movieId, rating));
                    } catch (NumberFormatException ignored) {
                        // Ignorar filas malformadas
                    }
                }
            }
        }
        return ratings;
    }

    /**
     * Divide una línea CSV respetando comillas dobles.
     */
    public static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
