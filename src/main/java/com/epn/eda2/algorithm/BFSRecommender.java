package com.epn.eda2.algorithm;

import com.epn.eda2.model.BipartiteGraph;
import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Recommendation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Algoritmo de Recomendación basado en Búsqueda en Anchura (BFS) sobre un Grafo Bipartito
 * explorando caminos de longitud 2 (Usuarios Similares) y longitud 3 (Películas Candidatas).
 *
 * Estructura de Niveles BFS:
 * - Nivel 0: Usuario objetivo u
 * - Nivel 1 (longitud 1): Películas p1 vistas por u
 * - Nivel 2 (longitud 2): Usuarios similares u' que también vieron p1
 * - Nivel 3 (longitud 3): Películas candidatas p2 vistas por u' que u aún NO ha visto (verificado en O(1) con HashSet)
 */
public class BFSRecommender {

    private final BipartiteGraph graph;

    public BFSRecommender(BipartiteGraph graph) {
        this.graph = graph;
    }

    /**
     * Calcula la similitud de Jaccard entre dos usuarios u y u' en base a sus conjuntos de películas vistas:
     * J(u, u') = |N(u) ∩ N(u')| / |N(u) ∪ N(u')|
     */
    public double computeJaccardSimilarity(int userU, int userUPrime) {
        Set<Integer> moviesU = graph.getMoviesWatchedByUser(userU);
        Set<Integer> moviesUPrime = graph.getMoviesWatchedByUser(userUPrime);
        if (moviesU.isEmpty() || moviesUPrime.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        Set<Integer> smaller = moviesU.size() <= moviesUPrime.size() ? moviesU : moviesUPrime;
        Set<Integer> larger = moviesU.size() <= moviesUPrime.size() ? moviesUPrime : moviesU;
        for (Integer m : smaller) {
            if (larger.contains(m)) {
                intersection++;
            }
        }
        int union = moviesU.size() + moviesUPrime.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * Ejecuta el recorrido BFS por niveles (0 -> 1 -> 2 -> 3) desde el usuario objetivo u
     * y devuelve el Top-K de películas recomendadas con su puntaje y explicación detallada.
     *
     * Complejidad Temporal:
     * - Nivel 1: O(|N(u)|)
     * - Nivel 2: O(sum_{p1 in N(u)} |N(p1)|)
     * - Nivel 3: O(sum_{u' in U'} |N(u')|) con filtro de exclusión en O(1) usando HashSet
     */
    public List<Recommendation> recommendTopK(int targetUserId, int k) {
        if (k <= 0) {
            return Collections.emptyList();
        }

        // Nivel 0 -> Nivel 1: Películas p1 vistas por u (HashSet para consulta O(1))
        Set<Integer> level1WatchedByU = new HashSet<>(graph.getMoviesWatchedByUser(targetUserId));
        if (level1WatchedByU.isEmpty()) {
            return Collections.emptyList();
        }

        // Nivel 1 -> Nivel 2: Usuarios similares u' que vieron al menos una película p1 de u
        // Guardamos qué películas puente p1 conectan a u con cada u'
        Map<Integer, Set<Integer>> similarUserToSharedP1 = new HashMap<>();
        for (Integer p1 : level1WatchedByU) {
            Set<Integer> usersWhoWatchedP1 = graph.getUsersWhoWatchedMovie(p1);
            for (Integer uPrime : usersWhoWatchedP1) {
                if (uPrime != targetUserId) {
                    similarUserToSharedP1
                            .computeIfAbsent(uPrime, id -> new LinkedHashSet<>())
                            .add(p1);
                }
            }
        }

        if (similarUserToSharedP1.isEmpty()) {
            return Collections.emptyList();
        }

        // Precalculamos la similitud de Jaccard J(u, u') para cada vecino de Nivel 2
        Map<Integer, Double> jaccardByUser = new HashMap<>();
        for (Integer uPrime : similarUserToSharedP1.keySet()) {
            jaccardByUser.put(uPrime, computeJaccardSimilarity(targetUserId, uPrime));
        }

        // Nivel 2 -> Nivel 3: Películas p2 vistas por u' que u aún NO ha visto (O(1) con HashSet)
        Map<Integer, Integer> pathCountByP2 = new HashMap<>();
        Map<Integer, Double> jaccardSumByP2 = new HashMap<>();
        Map<Integer, Set<Integer>> bridgeMoviesByP2 = new HashMap<>();
        Map<Integer, Set<Integer>> similarUsersByP2 = new HashMap<>();

        for (Map.Entry<Integer, Set<Integer>> entry : similarUserToSharedP1.entrySet()) {
            int uPrime = entry.getKey();
            Set<Integer> sharedP1Set = entry.getValue();
            double jaccard = jaccardByUser.getOrDefault(uPrime, 0.0);

            Set<Integer> moviesOfUPrime = graph.getMoviesWatchedByUser(uPrime);
            for (Integer p2 : moviesOfUPrime) {
                // Verificación O(1) en HashSet: descartar si el usuario objetivo u ya vio p2
                if (level1WatchedByU.contains(p2)) {
                    continue;
                }

                // Número de caminos de longitud 3 (u -> p1 -> u' -> p2) aportados por este u'
                int pathsThroughUPrime = sharedP1Set.size();
                pathCountByP2.merge(p2, pathsThroughUPrime, Integer::sum);

                // Puntaje ponderado por caminos y similitud de Jaccard
                double contribution = pathsThroughUPrime * (1.0 + jaccard);
                jaccardSumByP2.merge(p2, contribution, Double::sum);

                bridgeMoviesByP2.computeIfAbsent(p2, id -> new LinkedHashSet<>()).addAll(sharedP1Set);
                similarUsersByP2.computeIfAbsent(p2, id -> new LinkedHashSet<>()).add(uPrime);
            }
        }

        // Construcción de objetos Recommendation y ordenamiento Top-K
        List<Recommendation> recommendations = new ArrayList<>(pathCountByP2.size());
        for (Integer p2 : pathCountByP2.keySet()) {
            Movie movie = graph.getMovie(p2);
            int paths = pathCountByP2.get(p2);
            double score = jaccardSumByP2.getOrDefault(p2, (double) paths);
            Set<Integer> bridges = bridgeMoviesByP2.getOrDefault(p2, Collections.emptySet());
            Set<Integer> simUsers = similarUsersByP2.getOrDefault(p2, Collections.emptySet());

            double avgJaccard = simUsers.stream()
                    .mapToDouble(uPrime -> jaccardByUser.getOrDefault(uPrime, 0.0))
                    .average()
                    .orElse(0.0);

            String explanation = buildExplanation(bridges, simUsers, paths, avgJaccard);
            recommendations.add(new Recommendation(
                    movie, score, paths, avgJaccard, bridges, simUsers, explanation
            ));
        }

        Collections.sort(recommendations);
        if (recommendations.size() > k) {
            return new ArrayList<>(recommendations.subList(0, k));
        }
        return recommendations;
    }

    /**
     * Genera la trazabilidad exacta en lenguaje natural:
     * "Recomendada porque viste X, al igual que los usuarios Y, Z"
     */
    private String buildExplanation(
            Set<Integer> bridgeMoviesP1,
            Set<Integer> similarUsersUPrime,
            int pathCount,
            double avgJaccard) {

        String moviesText = bridgeMoviesP1.stream()
                .limit(3)
                .map(id -> "\"" + graph.getMovie(id).getTitle() + "\"")
                .collect(Collectors.joining(", "));
        if (bridgeMoviesP1.size() > 3) {
            moviesText += " (+" + (bridgeMoviesP1.size() - 3) + " más)";
        }

        String usersText = similarUsersUPrime.stream()
                .sorted()
                .limit(4)
                .map(id -> "U" + id)
                .collect(Collectors.joining(", "));
        if (similarUsersUPrime.size() > 4) {
            usersText += " (+" + (similarUsersUPrime.size() - 4) + " más)";
        }

        return String.format(
                Locale.US,
                "Recomendada porque viste %s, al igual que los usuarios %s (%d caminos | Similitud Jaccard: %.2f)",
                moviesText, usersText, pathCount, avgJaccard
        );
    }

    /**
     * Construye una instantánea (snapshot) del subgrafo local BFS de 4 niveles (u -> p1 -> u' -> p2)
     * para ser dibujada paso a paso en la Pestaña 2 (Visualizador Interactivo para Defensa Oral),
     * garantizando que el Nivel 3 contenga exactamente el Top-K solicitado (o el máximo disponible si hay menos de K).
     */
    public BFSSubgraphSnapshot buildVisualSubgraph(int targetUserId, int maxContextNodesPerColumn, int topK) {
        int safeMaxContext = Math.max(1, maxContextNodesPerColumn);
        int safeK = Math.max(1, topK);

        Set<Integer> allP1 = graph.getMoviesWatchedByUser(targetUserId);
        List<Integer> level1Movies = allP1.stream()
                .sorted()
                .limit(safeMaxContext)
                .collect(Collectors.toList());

        Map<Integer, Integer> uPrimeSharedCount = new HashMap<>();
        // Consideramos todos los usuarios similares conectados desde las películas vistas por u
        for (Integer p1 : allP1) {
            for (Integer uPrime : graph.getUsersWhoWatchedMovie(p1)) {
                if (uPrime != targetUserId) {
                    uPrimeSharedCount.merge(uPrime, 1, Integer::sum);
                }
            }
        }

        List<Integer> level2Users = uPrimeSharedCount.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : Integer.compare(a.getKey(), b.getKey());
                })
                .limit(safeMaxContext)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Nivel 3 sincronizado exactamente con el parámetro Top-K seleccionado
        List<Recommendation> topRecs = recommendTopK(targetUserId, safeK);
        List<Integer> level3Movies = topRecs.stream()
                .map(Recommendation::getMovieId)
                .collect(Collectors.toList());

        // Aristas Nivel 1 -> Nivel 2 (p1 -> u')
        Map<Integer, Set<Integer>> edgesP1ToUPrime = new LinkedHashMap<>();
        for (Integer p1 : level1Movies) {
            Set<Integer> connectedUPrime = new LinkedHashSet<>();
            Set<Integer> watchers = graph.getUsersWhoWatchedMovie(p1);
            for (Integer uPrime : level2Users) {
                if (watchers.contains(uPrime)) {
                    connectedUPrime.add(uPrime);
                }
            }
            edgesP1ToUPrime.put(p1, connectedUPrime);
        }

        // Aristas Nivel 2 -> Nivel 3 (u' -> p2)
        Map<Integer, Set<Integer>> edgesUPrimeToP2 = new LinkedHashMap<>();
        for (Integer uPrime : level2Users) {
            Set<Integer> connectedP2 = new LinkedHashSet<>();
            Set<Integer> watched = graph.getMoviesWatchedByUser(uPrime);
            for (Integer p2 : level3Movies) {
                if (watched.contains(p2)) {
                    connectedP2.add(p2);
                }
            }
            edgesUPrimeToP2.put(uPrime, connectedP2);
        }

        return new BFSSubgraphSnapshot(
                targetUserId,
                level1Movies,
                level2Users,
                level3Movies,
                edgesP1ToUPrime,
                edgesUPrimeToP2,
                topRecs
        );
    }

    /**
     * Sobrecarga compatible que utiliza topK tanto para el límite de contexto como para el Nivel 3.
     */
    public BFSSubgraphSnapshot buildVisualSubgraph(int targetUserId, int topK) {
        return buildVisualSubgraph(targetUserId, Math.max(6, topK), topK);
    }

    /**
     * Estructura inmutable con los nodos y aristas de los 4 niveles BFS para el visualizador gráfico.
     */
    public static class BFSSubgraphSnapshot {
        private final int targetUser;
        private final List<Integer> level1Movies;
        private final List<Integer> level2Users;
        private final List<Integer> level3Movies;
        private final Map<Integer, Set<Integer>> edgesP1ToUPrime;
        private final Map<Integer, Set<Integer>> edgesUPrimeToP2;
        private final List<Recommendation> recommendations;

        public BFSSubgraphSnapshot(
                int targetUser,
                List<Integer> level1Movies,
                List<Integer> level2Users,
                List<Integer> level3Movies,
                Map<Integer, Set<Integer>> edgesP1ToUPrime,
                Map<Integer, Set<Integer>> edgesUPrimeToP2,
                List<Recommendation> recommendations) {
            this.targetUser = targetUser;
            this.level1Movies = level1Movies;
            this.level2Users = level2Users;
            this.level3Movies = level3Movies;
            this.edgesP1ToUPrime = edgesP1ToUPrime;
            this.edgesUPrimeToP2 = edgesUPrimeToP2;
            this.recommendations = recommendations;
        }

        public int getTargetUser() {
            return targetUser;
        }

        public List<Integer> getLevel1Movies() {
            return level1Movies;
        }

        public List<Integer> getLevel2Users() {
            return level2Users;
        }

        public List<Integer> getLevel3Movies() {
            return level3Movies;
        }

        public Map<Integer, Set<Integer>> getEdgesP1ToUPrime() {
            return edgesP1ToUPrime;
        }

        public Map<Integer, Set<Integer>> getEdgesUPrimeToP2() {
            return edgesUPrimeToP2;
        }

        public List<Recommendation> getRecommendations() {
            return recommendations;
        }
    }
}
