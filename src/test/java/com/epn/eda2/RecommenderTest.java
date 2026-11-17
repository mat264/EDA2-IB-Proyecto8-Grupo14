package com.epn.eda2;

import com.epn.eda2.algorithm.BFSRecommender;
import com.epn.eda2.algorithm.BFSRecommender.BFSSubgraphSnapshot;
import com.epn.eda2.metrics.BenchmarkResult;
import com.epn.eda2.metrics.Evaluator;
import com.epn.eda2.model.BipartiteGraph;
import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Rating;
import com.epn.eda2.model.Recommendation;
import com.epn.eda2.util.CsvDatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de pruebas unitarias automatizadas (JUnit 5) para validar el Grafo Bipartito,
 * el algoritmo de recomendación BFS por niveles y el módulo de evaluación Train/Test 80/20.
 */
class RecommenderTest {

    private List<Movie> syntheticMovies;
    private List<Rating> syntheticRatings;
    private BipartiteGraph graph;
    private BFSRecommender recommender;

    @BeforeEach
    void setUp() throws IOException {
        syntheticMovies = CsvDatasetLoader.loadMoviesPortable(
                null, CsvDatasetLoader.SYNTHETIC_MOVIES_RESOURCE);
        syntheticRatings = CsvDatasetLoader.loadRatingsPortable(
                null, CsvDatasetLoader.SYNTHETIC_RATINGS_RESOURCE);

        graph = new BipartiteGraph();
        graph.buildFromDataset(
                syntheticMovies,
                syntheticRatings,
                BipartiteGraph.DEFAULT_POSITIVE_RATING_THRESHOLD
        );
        recommender = new BFSRecommender(graph);
    }

    @Test
    @DisplayName("1. Verifica que se carguen correctamente los usuarios, películas y aristas sin duplicados")
    void testBipartiteGraphConstruction() {
        // El dataset sintético tiene 6 usuarios, 8 películas y 23 calificaciones >= 4.0 (1 calificación es 3.0)
        assertEquals(6, graph.getUserCount(), "El grafo debe contener exactamente 6 usuarios activos.");
        assertEquals(8, graph.getMovieCount(), "El catálogo del grafo debe contener exactamente 8 películas.");
        assertEquals(23, graph.getEdgeCount(), "El grafo debe registrar exactamente 23 aristas con rating >= 4.0.");

        // Verificar que reinsertar una arista existente no duplique el conteo de aristas en los conjuntos HashSet
        int edgesBefore = graph.getEdgeCount();
        graph.addInteraction(1, 1, 5.0);
        assertEquals(edgesBefore, graph.getEdgeCount(), "No deben existir aristas duplicadas en el grafo bipartito.");
        assertTrue(graph.hasUserWatchedMovie(1, 1), "La consulta O(1) debe confirmar que U1 vio la película 1.");
        assertFalse(graph.hasUserWatchedMovie(4, 7), "La interacción con rating 3.0 (< 4.0) debe ser filtrada.");
    }

    @Test
    @DisplayName("2. Ejecuta recomendaciones con K=2 y K=4 y verifica que el tamaño nunca exceda K")
    void testTopKRespectsLimit() {
        // Para el usuario U1 (vio películas 1, 2, 3), existen 5 películas candidatas alcanzables en Nivel 3 (4, 5, 6, 7, 8)
        List<Recommendation> recsK2 = recommender.recommendTopK(1, 2);
        assertEquals(2, recsK2.size(), "Con K=2, la lista de recomendaciones debe tener exactamente 2 elementos.");

        List<Recommendation> recsK4 = recommender.recommendTopK(1, 4);
        assertEquals(4, recsK4.size(), "Con K=4, la lista de recomendaciones debe tener exactamente 4 elementos.");
        assertTrue(recsK4.size() <= 4, "El tamaño de la lista devuelta nunca debe exceder K.");

        // Verificar además que el subgrafo del Visualizador (Pestaña 2) sincronice exactamente K en Nivel 3
        BFSSubgraphSnapshot snapshotK2 = recommender.buildVisualSubgraph(1, 6, 2);
        assertEquals(2, snapshotK2.getLevel3Movies().size(),
                "El Nivel 3 del subgrafo visualizador debe reflejar exactamente K=2 películas recomendadas.");

        BFSSubgraphSnapshot snapshotK4 = recommender.buildVisualSubgraph(1, 6, 4);
        assertEquals(4, snapshotK4.getLevel3Movies().size(),
                "El Nivel 3 del subgrafo visualizador debe reflejar exactamente K=4 películas recomendadas.");
    }

    @Test
    @DisplayName("3. Comprueba que ninguna película recomendada en Nivel 3 pertenezca a las ya vistas en Nivel 1")
    void testNoAlreadyWatchedMoviesRecommended() {
        for (Integer userId : graph.getAllUserIds()) {
            Set<Integer> watchedLevel1 = graph.getMoviesWatchedByUser(userId);
            List<Recommendation> recommendationsLevel3 = recommender.recommendTopK(userId, 10);

            for (Recommendation rec : recommendationsLevel3) {
                assertFalse(
                        watchedLevel1.contains(rec.getMovieId()),
                        "La película recomendada ID=" + rec.getMovieId()
                                + " ya fue vista por el usuario U" + userId + " y no debe recomendarse."
                );
            }
        }
    }

    @Test
    @DisplayName("4. Prueba usuario inexistente o sin películas calificadas (Cold-Start) sin NullPointerException")
    void testColdStartAndInvalidUserHandling() {
        // Usuario inexistente (ID 9999)
        List<Recommendation> nonExistentUserRecs = assertDoesNotThrow(
                () -> recommender.recommendTopK(9999, 5),
                "Consultar un usuario inexistente no debe lanzar excepción."
        );
        assertNotNull(nonExistentUserRecs);
        assertTrue(nonExistentUserRecs.isEmpty(), "Un usuario inexistente debe recibir una lista vacía.");

        // Usuario registrado sin películas calificadas (Cold-Start)
        graph.addUser(500);
        List<Recommendation> coldStartRecs = assertDoesNotThrow(
                () -> recommender.recommendTopK(500, 5),
                "Consultar un usuario sin historial no debe lanzar NullPointerException."
        );
        assertNotNull(coldStartRecs);
        assertTrue(coldStartRecs.isEmpty(), "Un usuario sin películas vistas debe recibir una lista vacía.");

        // K inválido (K = 0 o negativo)
        assertTrue(recommender.recommendTopK(1, 0).isEmpty(), "Con K=0 debe retornar una lista vacía.");
    }

    @Test
    @DisplayName("5. Ejecuta el split 80/20 y verifica que Precision@K, Recall@K y tiempos estén en rangos válidos")
    void testEvaluatorMetricsWithinBounds() {
        BenchmarkResult result = Evaluator.evaluateTrainTest80_20(
                syntheticMovies,
                syntheticRatings,
                BipartiteGraph.DEFAULT_POSITIVE_RATING_THRESHOLD,
                3
        );

        assertNotNull(result);
        assertEquals(3, result.getK());
        assertTrue(result.getPrecisionAtK() >= 0.0 && result.getPrecisionAtK() <= 1.0,
                "Precision@K debe estar en el rango [0.0, 1.0]. Valor actual: " + result.getPrecisionAtK());
        assertTrue(result.getRecallAtK() >= 0.0 && result.getRecallAtK() <= 1.0,
                "Recall@K debe estar en el rango [0.0, 1.0]. Valor actual: " + result.getRecallAtK());
        assertTrue(result.getGraphBuildTimeMs() > 0.0,
                "El tiempo de construcción del grafo debe ser positivo.");
        assertTrue(result.getAvgRecommendationTimeMs() > 0.0,
                "El tiempo promedio de recomendación debe ser positivo.");
        assertTrue(result.getMemoryUsedMb() > 0.0,
                "La memoria reportada debe ser positiva.");
        assertTrue(result.getEvaluatedUsers() > 0,
                "Debe haberse evaluado al menos un usuario en la partición 80/20.");
    }
}
