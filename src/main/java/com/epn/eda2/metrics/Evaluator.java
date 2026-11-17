package com.epn.eda2.metrics;

import com.epn.eda2.algorithm.BFSRecommender;
import com.epn.eda2.model.BipartiteGraph;
import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Rating;
import com.epn.eda2.model.Recommendation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Módulo de Evaluación Automatizada y Benchmarks de Rendimiento.
 * Realiza una partición Train/Test (80% / 20%) por usuario sobre las calificaciones positivas (>= 4.0)
 * y calcula:
 * - Precision@K
 * - Recall@K
 * - Tiempo de construcción del grafo (ms)
 * - Tiempo promedio de recomendación por usuario (ms)
 * - Memoria utilizada en la JVM (MB) mediante Runtime.getRuntime()
 */
public class Evaluator {

    private static final long FIXED_SEED = 42L;

    /**
     * Ejecuta el protocolo de validación Train/Test 80/20 sobre los datos cargados.
     */
    public static BenchmarkResult evaluateTrainTest80_20(
            Collection<Movie> movies,
            List<Rating> allRatings,
            double minPositiveRating,
            int k) {

        int safeK = Math.max(1, k);

        // 1. Filtrar ratings positivos (>= 4.0) y agruparlos por usuario
        Map<Integer, List<Rating>> positiveByUser = new HashMap<>();
        for (Rating r : allRatings) {
            if (r.getRating() >= minPositiveRating) {
                positiveByUser.computeIfAbsent(r.getUserId(), id -> new ArrayList<>()).add(r);
            }
        }

        // 2. División 80% Train / 20% Test por usuario (con semilla determinista para reproducibilidad)
        List<Rating> trainRatings = new ArrayList<>();
        Map<Integer, Set<Integer>> testMoviesByUser = new HashMap<>();
        Random rng = new Random(FIXED_SEED);
        int totalTestEdges = 0;

        List<Integer> sortedUsers = new ArrayList<>(positiveByUser.keySet());
        Collections.sort(sortedUsers);

        for (Integer userId : sortedUsers) {
            List<Rating> userInteractions = new ArrayList<>(positiveByUser.get(userId));
            Collections.shuffle(userInteractions, rng);

            int n = userInteractions.size();
            if (n < 2) {
                // Si el usuario solo tiene 1 interacción, se envía a entrenamiento
                trainRatings.addAll(userInteractions);
                continue;
            }

            // Al menos 1 ítem en Test (20% redondeado hacia arriba cuando n >= 2)
            int testCount = Math.max(1, (int) Math.round(n * 0.20));
            if (testCount >= n) {
                testCount = n - 1;
            }

            Set<Integer> testSet = new HashSet<>();
            for (int i = 0; i < n; i++) {
                Rating r = userInteractions.get(i);
                if (i < testCount) {
                    testSet.add(r.getMovieId());
                    totalTestEdges++;
                } else {
                    trainRatings.add(r);
                }
            }
            if (!testSet.isEmpty()) {
                testMoviesByUser.put(userId, testSet);
            }
        }

        // 3. Medir tiempo de construcción del grafo de entrenamiento (ms)
        long startBuildNano = System.nanoTime();
        BipartiteGraph trainGraph = new BipartiteGraph();
        trainGraph.buildFromDataset(movies, trainRatings, minPositiveRating);
        long endBuildNano = System.nanoTime();
        double graphBuildTimeMs = (endBuildNano - startBuildNano) / 1_000_000.0;

        // 4. Evaluar recomendaciones BFS para cada usuario con conjunto de prueba
        BFSRecommender recommender = new BFSRecommender(trainGraph);
        double sumPrecision = 0.0;
        double sumRecall = 0.0;
        int evaluatedUsers = 0;

        long startRecNano = System.nanoTime();
        for (Map.Entry<Integer, Set<Integer>> entry : testMoviesByUser.entrySet()) {
            int userId = entry.getKey();
            Set<Integer> groundTruthTestItems = entry.getValue();
            if (groundTruthTestItems.isEmpty()) {
                continue;
            }

            List<Recommendation> topKRecs = recommender.recommendTopK(userId, safeK);
            int hits = 0;
            for (Recommendation rec : topKRecs) {
                if (groundTruthTestItems.contains(rec.getMovieId())) {
                    hits++;
                }
            }

            double precisionU = (double) hits / safeK;
            double recallU = (double) hits / groundTruthTestItems.size();

            sumPrecision += precisionU;
            sumRecall += recallU;
            evaluatedUsers++;
        }
        long endRecNano = System.nanoTime();

        double totalRecTimeMs = (endRecNano - startRecNano) / 1_000_000.0;
        double avgRecommendationTimeMs = evaluatedUsers > 0 ? totalRecTimeMs / evaluatedUsers : 0.0;
        double meanPrecisionAtK = evaluatedUsers > 0 ? sumPrecision / evaluatedUsers : 0.0;
        double meanRecallAtK = evaluatedUsers > 0 ? sumRecall / evaluatedUsers : 0.0;

        // 5. Medir memoria utilizada por la JVM en MB usando Runtime.getRuntime()
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsedMb = usedBytes / (1024.0 * 1024.0);

        return new BenchmarkResult(
                safeK,
                meanPrecisionAtK,
                meanRecallAtK,
                graphBuildTimeMs,
                avgRecommendationTimeMs,
                memoryUsedMb,
                evaluatedUsers,
                trainGraph.getEdgeCount(),
                totalTestEdges
        );
    }
}
