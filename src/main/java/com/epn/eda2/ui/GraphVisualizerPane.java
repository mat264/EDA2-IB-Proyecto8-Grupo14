package com.epn.eda2.ui;

import com.epn.eda2.algorithm.BFSRecommender;
import com.epn.eda2.algorithm.BFSRecommender.BFSSubgraphSnapshot;
import com.epn.eda2.model.BipartiteGraph;
import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Recommendation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pestaña 2: Visualizador Interactivo del Subgrafo Bipartito Local en 4 Columnas Verticales
 * optimizado para Proyector en Defensa Oral Universitaria.
 *
 * Incluye:
 * - Espaciado vertical dinámico con margen inferior libre (>= 30px) sin solapamientos.
 * - Transiciones suaves (FadeTransition + ScaleTransition de 350ms) al avanzar cada nivel BFS.
 * - Explicaciones en lenguaje natural en 3 puntos directos con complejidad Big-O simplificada.
 */
public class GraphVisualizerPane extends BorderPane {

    private static final int MAX_NODES_PER_COLUMN = 12;

    // Paleta de colores oficial y consistente
    private static final Color COLOR_TARGET_USER = Color.web("#2563EB");   // Azul
    private static final Color COLOR_WATCHED_MOVIES = Color.web("#0D9488"); // Verde Esmeralda
    private static final Color COLOR_SIMILAR_USERS = Color.web("#7C3AED");  // Morado
    private static final Color COLOR_RECOMMENDATIONS = Color.web("#EA580C");// Naranja

    private static final String CARD_STYLE =
            "-fx-background-color: #FFFFFF; "
            + "-fx-border-color: #CBD5E1; "
            + "-fx-border-radius: 8px; "
            + "-fx-background-radius: 8px; "
            + "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.07), 10, 0, 0, 2);";

    private BipartiteGraph graph;
    private BFSRecommender recommender;
    private int currentUserId = 1;
    private int currentTopK = 5;
    private int currentBfsLevel = -1; // -1 = En espera, 0 = Paso 1, 1 = Paso 2, 2 = Paso 3, 3 = Paso 4
    private BFSSubgraphSnapshot snapshot;

    private final Pane canvasPane;
    private final Label statusStepLabel;
    private final TextArea complexityExplanationArea;
    private final Button nextStepButton;
    private final Button resetButton;

    public GraphVisualizerPane(BipartiteGraph graph) {
        this.graph = graph;
        this.recommender = new BFSRecommender(graph);

        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F8FAFC;");

        // Barra superior de controles interactivos para la Defensa Oral
        nextStepButton = new Button("Paso Siguiente (BFS) ▶");
        nextStepButton.setStyle(
                "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 9 18;");
        nextStepButton.setOnAction(e -> advanceBfsStep());

        resetButton = new Button("Reiniciar ↺");
        resetButton.setStyle(
                "-fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 9 16;");
        resetButton.setOnAction(e -> resetBfsStep());

        statusStepLabel = new Label("Vista General del Grafo — Presione 'Paso Siguiente (BFS)' para iniciar");
        statusStepLabel.getStyleClass().add("section-header");
        statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        HBox controlsBar = new HBox(16, nextStepButton, resetButton, statusStepLabel);
        controlsBar.setAlignment(Pos.CENTER_LEFT);
        controlsBar.setPadding(new Insets(12, 16, 12, 16));
        controlsBar.setStyle(CARD_STYLE);
        setTop(controlsBar);

        // Lienzo central donde se dibujan las 4 columnas verticales del grafo bipartito
        canvasPane = new Pane();
        canvasPane.setPrefSize(960, 460);
        canvasPane.setMinHeight(380);
        canvasPane.setStyle(CARD_STYLE);
        BorderPane.setMargin(canvasPane, new Insets(12, 0, 12, 0));

        canvasPane.widthProperty().addListener((obs, oldV, newV) -> redrawGraph(false));
        canvasPane.heightProperty().addListener((obs, oldV, newV) -> redrawGraph(false));
        setCenter(canvasPane);

        // Panel inferior de explicación en lenguaje natural y complejidad Big-O simplificada
        Label expTitle = new Label("Explicación del Algoritmo para Defensa Oral (Lectura Directa):");
        expTitle.getStyleClass().add("section-header");
        expTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        complexityExplanationArea = new TextArea();
        complexityExplanationArea.setEditable(false);
        complexityExplanationArea.setWrapText(true);
        complexityExplanationArea.setPrefRowCount(4);
        complexityExplanationArea.setPrefHeight(125);
        complexityExplanationArea.setStyle(
                "-fx-control-inner-background: #0F172A; "
                + "-fx-text-fill: #F8FAFC; "
                + "-fx-font-family: 'System'; "
                + "-fx-font-size: 15px; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 8px; "
                + "-fx-border-radius: 8px;");

        VBox bottomBox = new VBox(8, expTitle, complexityExplanationArea);
        VBox.setVgrow(complexityExplanationArea, Priority.NEVER);
        setBottom(bottomBox);

        refreshForUserAndK(currentUserId, currentTopK);
    }

    /**
     * Actualiza la referencia al grafo activo, el usuario objetivo y el parámetro Top-K,
     * reiniciando el visualizador al paso inicial (-1) para que el Nivel 3 refleje exactamente K películas.
     */
    public void updateGraphUserAndK(BipartiteGraph newGraph, int userId, int topK) {
        this.graph = newGraph;
        this.recommender = new BFSRecommender(newGraph);
        refreshForUserAndK(userId, topK);
    }

    /**
     * Actualiza la referencia al grafo activo y reconstruye el subgrafo para el usuario seleccionado.
     */
    public void updateGraphAndUser(BipartiteGraph newGraph, int userId) {
        updateGraphUserAndK(newGraph, userId, this.currentTopK);
    }

    /**
     * Cambia el usuario objetivo y el límite Top-K del visualizador y reinicia el recorrido BFS al paso inicial.
     */
    public void refreshForUserAndK(int userId, int topK) {
        this.currentUserId = userId;
        this.currentTopK = Math.max(1, topK);
        this.currentBfsLevel = -1;
        this.snapshot = recommender.buildVisualSubgraph(userId, MAX_NODES_PER_COLUMN, this.currentTopK);
        updateStepDescription();
        redrawGraph(false);
    }

    /**
     * Cambia el usuario objetivo del visualizador y reinicia el recorrido BFS.
     */
    public void refreshForUser(int userId) {
        refreshForUserAndK(userId, this.currentTopK);
    }

    /**
     * Avanza al siguiente paso del recorrido BFS animando suavemente el nuevo nivel descubierto.
     */
    private void advanceBfsStep() {
        if (currentBfsLevel < 3) {
            currentBfsLevel++;
        } else {
            currentBfsLevel = 0;
        }
        updateStepDescription();
        redrawGraph(true);
    }

    /**
     * Reinicia la iluminación del recorrido BFS al estado inicial.
     */
    private void resetBfsStep() {
        currentBfsLevel = -1;
        updateStepDescription();
        redrawGraph(false);
    }

    /**
     * Actualiza el título del paso (Paso 1 de 4 ... Paso 4 de 4) y las 3 líneas claras
     * en español sencillo sin fórmulas crípticas.
     */
    private void updateStepDescription() {
        if (snapshot == null) {
            return;
        }
        int u = snapshot.getTargetUser();
        List<Integer> watchedList = snapshot.getLevel1Movies();
        List<Integer> similarUsersList = snapshot.getLevel2Users();
        List<Recommendation> recList = snapshot.getRecommendations();

        switch (currentBfsLevel) {
            case -1 -> {
                statusStepLabel.setText("Vista Previa del Subgrafo — Usuario U" + u + " (Presione 'Paso Siguiente')");
                statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #334155;");
                complexityExplanationArea.setText(
                        "1. Qué hace el algoritmo: Modela usuarios y películas en un Grafo Bipartito y busca recomendaciones en 4 niveles.\n"
                        + "2. Estructura de datos usada: Dos tablas HashMap con conjuntos HashSet para conectar usuarios y películas en tiempo O(1).\n"
                        + "3. Complejidad Big-O simplificada: Presione 'Paso Siguiente (BFS) ▶' para iniciar en el Paso 1 de 4."
                );
            }
            case 0 -> {
                statusStepLabel.setText("Paso 1 de 4: Usuario Objetivo (U" + u + ")");
                statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
                complexityExplanationArea.setText(
                        "1. Qué hace el algoritmo: Ubicamos al usuario objetivo U" + u + " como punto de partida del recorrido en anchura (BFS).\n"
                        + "2. Estructura de datos usada: Consulta directa en HashMap por el ID del usuario en tiempo constante O(1).\n"
                        + "3. Complejidad Big-O simplificada: O(1) (Acceso directo por ID en tabla hash)."
                );
            }
            case 1 -> {
                String watchedNames = watchedList.stream()
                        .limit(4)
                        .map(id -> "\"" + truncate(graph.getMovie(id).getTitle(), 20) + "\"")
                        .collect(Collectors.joining(", "));
                statusStepLabel.setText("Paso 2 de 4: Películas Vistas (Nivel 1 — " + watchedList.size() + " películas)");
                statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0D9488;");
                complexityExplanationArea.setText(
                        "1. Qué hace el algoritmo: Obtenemos las películas que U" + u + " ya vio y calificó positivamente (" + watchedNames + ").\n"
                        + "2. Estructura de datos usada: Se guardan en un HashSet para poder verificar después en O(1) qué películas ya fueron vistas.\n"
                        + "3. Complejidad Big-O simplificada: O(V_u) donde V_u es el número de películas vistas por el usuario."
                );
            }
            case 2 -> {
                String simNames = similarUsersList.stream()
                        .map(id -> String.format(Locale.US, "U%d (Jaccard %.2f)", id, recommender.computeJaccardSimilarity(u, id)))
                        .collect(Collectors.joining(", "));
                statusStepLabel.setText("Paso 3 de 4: Usuarios Similares (Nivel 2 — " + similarUsersList.size() + " vecinos)");
                statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #7C3AED;");
                complexityExplanationArea.setText(
                        "1. Qué hace el algoritmo: Buscamos otros usuarios con gustos parecidos que también vieron las películas de U" + u
                        + " (" + (simNames.isEmpty() ? "Ninguno" : simNames) + ").\n"
                        + "2. Estructura de datos usada: Consulta directa en HashMap (movieToUsers) y cálculo de similitud de Jaccard entre conjuntos.\n"
                        + "3. Complejidad Big-O simplificada: O(V_u * U_p) recorriendo vecinos de cada película vista."
                );
            }
            case 3 -> {
                String recNames = recList.stream()
                        .limit(3)
                        .map(r -> String.format(Locale.US, "\"%s\" (Puntaje %.2f)", truncate(r.getTitle(), 20), r.getScore()))
                        .collect(Collectors.joining(", "));
                statusStepLabel.setText("Paso 4 de 4: Recomendaciones Finales (Nivel 3 — Top " + recList.size() + ")");
                statusStepLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #EA580C;");
                complexityExplanationArea.setText(
                        "1. Qué hace el algoritmo: Recolectamos las películas vistas por los usuarios similares que U" + u
                        + " aún NO ha visto (" + (recNames.isEmpty() ? "Sin candidatas" : recNames) + ").\n"
                        + "2. Estructura de datos usada: Filtro rápido en HashSet en O(1) para descartar películas ya vistas y ordenamiento de candidatas.\n"
                        + "3. Complejidad Big-O simplificada: O(K_vecinos * D + C log C) donde D es el promedio de películas por vecino y C log C es el ordenamiento del Top-K."
                );
            }
            default -> {
            }
        }
    }

    /**
     * Dibuja en el Pane las 4 columnas verticales con ajuste dinámico de altura/radio y margen inferior
     * garantizado (>= 30px) para evitar cualquier solapamiento.
     *
     * @param animateLatestLevel true cuando se presiona "Paso Siguiente (BFS)" para animar el nivel recién descubierto.
     */
    private void redrawGraph(boolean animateLatestLevel) {
        canvasPane.getChildren().clear();
        if (snapshot == null) {
            return;
        }

        double width = Math.max(canvasPane.getWidth(), 820);
        double height = Math.max(canvasPane.getHeight(), 380);

        double col0X = width * 0.11;
        double col1X = width * 0.36;
        double col2X = width * 0.63;
        double col3X = width * 0.88;

        // Encabezados autoexplicativos de las 4 columnas verticales (sin símbolos crípticos p1, u', p2)
        drawColumnHeader(col0X, "Paso 1: Nivel 0", "Usuario Objetivo", currentBfsLevel >= 0, COLOR_TARGET_USER);
        drawColumnHeader(col1X, "Paso 2: Nivel 1", "Películas Vistas", currentBfsLevel >= 1, COLOR_WATCHED_MOVIES);
        drawColumnHeader(col2X, "Paso 3: Nivel 2", "Usuarios Similares", currentBfsLevel >= 2, COLOR_SIMILAR_USERS);
        drawColumnHeader(col3X, "Paso 4: Nivel 3", "Películas Candidatas", currentBfsLevel >= 3, COLOR_RECOMMENDATIONS);

        // Márgenes superior e inferior garantizando al menos 34px libres abajo para que las etiquetas nunca toquen el borde
        double topHeaderArea = 70.0;
        double bottomSafeMargin = 36.0;

        List<Integer> watchedMovies = snapshot.getLevel1Movies();
        List<Integer> similarUsers = snapshot.getLevel2Users();
        List<Integer> candidateMovies = snapshot.getLevel3Movies();

        // Radio dinámico para Usuarios Similares (Nivel 2) según cantidad de nodos y altura del Pane
        double availHeight = Math.max(220.0, height - topHeaderArea - bottomSafeMargin);
        int maxUserCount = Math.max(1, similarUsers.size());
        double maxSlotPerUser = availHeight / maxUserCount;
        double dynamicUserRadius = Math.max(12.5, Math.min(21.0, (maxSlotPerUser - 17.0) * 0.44));

        // Altura dinámica independiente para tarjetas de Películas Vistas (Nivel 1) y Candidatas (Nivel 3)
        int watchedCount = Math.max(1, watchedMovies.size());
        double watchedBoxHeight = Math.max(26.0, Math.min(44.0, (availHeight / watchedCount) - 7.0));

        int candidateCount = Math.max(1, candidateMovies.size());
        double candidateBoxHeight = Math.max(24.0, Math.min(44.0, (availHeight / candidateCount) - 7.0));

        // Posiciones centradas verticalmente respetando márgenes seguros arriba y abajo
        Point2D uPos = new Point2D(col0X, topHeaderArea + availHeight * 0.50);

        Map<Integer, Point2D> watchedPositions = computeSafeColumnPositions(
                watchedMovies, col1X, topHeaderArea, height - bottomSafeMargin, watchedBoxHeight / 2.0 + 4.0);

        // Para los círculos de usuarios similares, reservamos 18px adicionales debajo de cada círculo para su etiqueta Jaccard
        Map<Integer, Point2D> similarUsersPositions = computeSafeColumnPositions(
                similarUsers, col2X, topHeaderArea + dynamicUserRadius + 4.0,
                height - bottomSafeMargin - 16.0, dynamicUserRadius);

        Map<Integer, Point2D> candidatePositions = computeSafeColumnPositions(
                candidateMovies, col3X, topHeaderArea, height - bottomSafeMargin, candidateBoxHeight / 2.0 + 4.0);

        // 1. Dibujar Aristas Nivel 0 -> Nivel 1 (Usuario Objetivo -> Películas Vistas)
        boolean highlightEdges0to1 = currentBfsLevel >= 1;
        boolean animateEdges0to1 = animateLatestLevel && currentBfsLevel == 1;
        for (Integer movieId : watchedMovies) {
            Point2D p1Pos = watchedPositions.get(movieId);
            if (p1Pos != null) {
                Line edge = createEdge(uPos, p1Pos, highlightEdges0to1, COLOR_WATCHED_MOVIES, currentBfsLevel == 1 ? 3.2 : 2.2);
                canvasPane.getChildren().add(edge);
                if (animateEdges0to1) {
                    playFadeOnly(edge);
                }
            }
        }

        // 2. Dibujar Aristas Nivel 1 -> Nivel 2 (Películas Vistas -> Usuarios Similares)
        boolean highlightEdges1to2 = currentBfsLevel >= 2;
        boolean animateEdges1to2 = animateLatestLevel && currentBfsLevel == 2;
        for (Map.Entry<Integer, Set<Integer>> entry : snapshot.getEdgesP1ToUPrime().entrySet()) {
            Point2D p1Pos = watchedPositions.get(entry.getKey());
            if (p1Pos == null) continue;
            for (Integer uPrime : entry.getValue()) {
                Point2D uPrimePos = similarUsersPositions.get(uPrime);
                if (uPrimePos != null) {
                    Line edge = createEdge(p1Pos, uPrimePos, highlightEdges1to2, COLOR_SIMILAR_USERS, currentBfsLevel == 2 ? 3.0 : 2.0);
                    canvasPane.getChildren().add(edge);
                    if (animateEdges1to2) {
                        playFadeOnly(edge);
                    }
                }
            }
        }

        // 3. Dibujar Aristas Nivel 2 -> Nivel 3 (Usuarios Similares -> Películas Candidatas)
        boolean highlightEdges2to3 = currentBfsLevel >= 3;
        boolean animateEdges2to3 = animateLatestLevel && currentBfsLevel == 3;
        for (Map.Entry<Integer, Set<Integer>> entry : snapshot.getEdgesUPrimeToP2().entrySet()) {
            Point2D uPrimePos = similarUsersPositions.get(entry.getKey());
            if (uPrimePos == null) continue;
            for (Integer p2 : entry.getValue()) {
                Point2D p2Pos = candidatePositions.get(p2);
                if (p2Pos != null) {
                    Line edge = createEdge(uPrimePos, p2Pos, highlightEdges2to3, COLOR_RECOMMENDATIONS, currentBfsLevel == 3 ? 3.2 : 2.0);
                    canvasPane.getChildren().add(edge);
                    if (animateEdges2to3) {
                        playFadeOnly(edge);
                    }
                }
            }
        }

        // 4. Dibujar Nodo Nivel 0: Usuario Objetivo
        Group targetUserGroup = createUserNodeGroup(
                uPos,
                22.0,
                "U" + snapshot.getTargetUser(),
                "Usuario Objetivo",
                "Usuario Objetivo U" + snapshot.getTargetUser() + " (Punto de partida del recorrido BFS)",
                currentBfsLevel >= 0,
                currentBfsLevel == 0,
                COLOR_TARGET_USER
        );
        canvasPane.getChildren().add(targetUserGroup);
        if (animateLatestLevel && currentBfsLevel == 0) {
            playNodeDiscoveryAnimation(targetUserGroup);
        }

        // 5. Dibujar Nodos Nivel 1: Películas Vistas
        for (Integer movieId : watchedMovies) {
            Point2D pos = watchedPositions.get(movieId);
            Movie m = graph.getMovie(movieId);
            double rating = graph.getRating(snapshot.getTargetUser(), movieId);
            Group movieGroup = createMovieNodeGroup(
                    pos,
                    162.0,
                    watchedBoxHeight,
                    truncate(m.getTitle(), 19),
                    String.format(Locale.US, "Vista (★ %.1f)", rating),
                    "Película Vista: " + m.getTitle() + "\nCalificación: ★ " + rating,
                    currentBfsLevel >= 1,
                    currentBfsLevel == 1,
                    COLOR_WATCHED_MOVIES
            );
            canvasPane.getChildren().add(movieGroup);
            if (animateLatestLevel && currentBfsLevel == 1) {
                playNodeDiscoveryAnimation(movieGroup);
            }
        }

        // 6. Dibujar Nodos Nivel 2: Usuarios Similares
        for (Integer uPrime : similarUsers) {
            Point2D pos = similarUsersPositions.get(uPrime);
            double jaccard = recommender.computeJaccardSimilarity(snapshot.getTargetUser(), uPrime);
            Group simUserGroup = createUserNodeGroup(
                    pos,
                    dynamicUserRadius,
                    "U" + uPrime,
                    String.format(Locale.US, "Jaccard: %.2f", jaccard),
                    String.format(Locale.US, "Usuario Similar U%d\nSimilitud de Jaccard con U%d: %.2f",
                            uPrime, snapshot.getTargetUser(), jaccard),
                    currentBfsLevel >= 2,
                    currentBfsLevel == 2,
                    COLOR_SIMILAR_USERS
            );
            canvasPane.getChildren().add(simUserGroup);
            if (animateLatestLevel && currentBfsLevel == 2) {
                playNodeDiscoveryAnimation(simUserGroup);
            }
        }

        // 7. Dibujar Nodos Nivel 3: Películas Candidatas / Recomendaciones
        Map<Integer, Recommendation> recMap = new HashMap<>();
        for (Recommendation rec : snapshot.getRecommendations()) {
            recMap.put(rec.getMovieId(), rec);
        }
        for (Integer p2 : candidateMovies) {
            Point2D pos = candidatePositions.get(p2);
            Movie m = graph.getMovie(p2);
            Recommendation rec = recMap.get(p2);
            String sub = rec != null
                    ? String.format(Locale.US, "Puntaje: %.2f (%d cam.)", rec.getScore(), rec.getPathCount())
                    : "Candidata";
            String tip = rec != null ? rec.getExplanation() : m.toString();
            Group recMovieGroup = createMovieNodeGroup(
                    pos,
                    168.0,
                    candidateBoxHeight,
                    truncate(m.getTitle(), 19),
                    sub,
                    tip,
                    currentBfsLevel >= 3,
                    currentBfsLevel == 3,
                    COLOR_RECOMMENDATIONS
            );
            canvasPane.getChildren().add(recMovieGroup);
            if (animateLatestLevel && currentBfsLevel == 3) {
                playNodeDiscoveryAnimation(recMovieGroup);
            }
        }
    }

    /**
     * Aplica una transición combinada (FadeTransition de 0.0 a 1.0 + ScaleTransition de 0.85 a 1.0 en 350ms)
     * sobre cada nodo recién descubierto en el paso actual del BFS.
     */
    private void playNodeDiscoveryAnimation(Node node) {
        node.setOpacity(0.0);
        node.setScaleX(0.85);
        node.setScaleY(0.85);

        FadeTransition fade = new FadeTransition(Duration.millis(350), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(350), node);
        scale.setFromX(0.85);
        scale.setFromY(0.85);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.play();
    }

    /**
     * Aplica un FadeTransition suave de 350ms a las aristas recién descubiertas.
     */
    private void playFadeOnly(Node edge) {
        edge.setOpacity(0.0);
        FadeTransition fade = new FadeTransition(Duration.millis(350), edge);
        fade.setFromValue(0.0);
        fade.setToValue(0.90);
        fade.play();
    }

    private void drawColumnHeader(double centerX, String title, String subtitle, boolean active, Color activeColor) {
        Text t1 = new Text(title);
        t1.setFont(Font.font("System", FontWeight.BOLD, 14.5));
        t1.setFill(active ? activeColor : Color.web("#64748B"));
        t1.setX(centerX - t1.getLayoutBounds().getWidth() / 2.0);
        t1.setY(26);

        Text t2 = new Text(subtitle);
        t2.setFont(Font.font("System", FontWeight.BOLD, 13.5));
        t2.setFill(active ? Color.web("#0F172A") : Color.web("#94A3B8"));
        t2.setX(centerX - t2.getLayoutBounds().getWidth() / 2.0);
        t2.setY(46);

        canvasPane.getChildren().addAll(t1, t2);
    }

    /**
     * Calcula las coordenadas Y de una columna garantizando que ni el primer ni el último nodo
     * invadan los márgenes superior o inferior.
     */
    private Map<Integer, Point2D> computeSafeColumnPositions(
            List<Integer> ids,
            double colX,
            double minY,
            double maxY,
            double nodeHalfExtent) {

        Map<Integer, Point2D> positions = new HashMap<>();
        int n = ids.size();
        if (n == 0) {
            return positions;
        }

        double safeStartY = minY + nodeHalfExtent;
        double safeEndY = Math.max(safeStartY, maxY - nodeHalfExtent);

        if (n == 1) {
            positions.put(ids.get(0), new Point2D(colX, (safeStartY + safeEndY) * 0.5));
            return positions;
        }

        double step = (safeEndY - safeStartY) / (n - 1);
        for (int i = 0; i < n; i++) {
            positions.put(ids.get(i), new Point2D(colX, safeStartY + step * i));
        }
        return positions;
    }

    private Line createEdge(Point2D from, Point2D to, boolean active, Color activeColor, double activeWidth) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        if (active) {
            line.setStroke(activeColor);
            line.setStrokeWidth(activeWidth);
            line.setOpacity(0.90);
        } else {
            line.setStroke(Color.web("#CBD5E1"));
            line.setStrokeWidth(1.4);
            line.getStrokeDashArray().addAll(5.0, 5.0);
            line.setOpacity(0.65);
        }
        return line;
    }

    private Group createUserNodeGroup(
            Point2D center,
            double radius,
            String label,
            String sublabel,
            String tooltipText,
            boolean active,
            boolean isCurrentStep,
            Color color) {

        Group group = new Group();
        double effectiveRadius = isCurrentStep ? radius + 2.5 : radius;

        Circle circle = new Circle(center.getX(), center.getY(), effectiveRadius);
        circle.setFill(active ? color : Color.web("#E2E8F0"));
        circle.setStroke(isCurrentStep ? Color.web("#0F172A") : (active ? color.darker() : Color.web("#94A3B8")));
        circle.setStrokeWidth(isCurrentStep ? 3.0 : 1.8);
        if (active) {
            circle.setEffect(new DropShadow(6, 0, 2, Color.rgb(15, 23, 42, 0.18)));
        }

        Text mainText = new Text(label);
        mainText.setFont(Font.font("System", FontWeight.BOLD, 13));
        mainText.setFill(active ? Color.WHITE : Color.web("#475569"));
        mainText.setX(center.getX() - mainText.getLayoutBounds().getWidth() / 2.0);
        mainText.setY(center.getY() + 4.5);

        Text subText = new Text(sublabel);
        subText.setFont(Font.font("System", FontWeight.BOLD, 12));
        subText.setFill(active ? Color.web("#0F172A") : Color.web("#64748B"));
        subText.setX(center.getX() - subText.getLayoutBounds().getWidth() / 2.0);
        subText.setY(center.getY() + effectiveRadius + 14.0);

        Tooltip tip = new Tooltip(tooltipText);
        tip.setStyle("-fx-font-size: 14px;");
        Tooltip.install(group, tip);

        group.getChildren().addAll(circle, mainText, subText);
        return group;
    }

    private Group createMovieNodeGroup(
            Point2D center,
            double boxWidth,
            double boxHeight,
            String titleText,
            String subTextStr,
            String tooltipText,
            boolean active,
            boolean isCurrentStep,
            Color color) {

        Group group = new Group();
        double x = center.getX() - boxWidth / 2.0;
        double y = center.getY() - boxHeight / 2.0;

        Rectangle rect = new Rectangle(x, y, boxWidth, boxHeight);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        rect.setFill(active ? color : Color.web("#F1F5F9"));
        rect.setStroke(isCurrentStep ? Color.web("#0F172A") : (active ? color.darker() : Color.web("#94A3B8")));
        rect.setStrokeWidth(isCurrentStep ? 3.0 : 1.6);
        if (active) {
            rect.setEffect(new DropShadow(6, 0, 2, Color.rgb(15, 23, 42, 0.18)));
        }

        Text t1 = new Text(titleText);
        t1.setFont(Font.font("System", FontWeight.BOLD, 12.5));
        t1.setFill(active ? Color.WHITE : Color.web("#334155"));
        t1.setX(x + 9);
        t1.setY(y + boxHeight * 0.42);

        Text t2 = new Text(subTextStr);
        t2.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11.5));
        t2.setFill(active ? Color.web("#FEF9C3") : Color.web("#64748B"));
        t2.setX(x + 9);
        t2.setY(y + boxHeight * 0.80);

        Tooltip tip = new Tooltip(tooltipText);
        tip.setWrapText(true);
        tip.setMaxWidth(420);
        tip.setStyle("-fx-font-size: 14px;");
        Tooltip.install(group, tip);

        group.getChildren().addAll(rect, t1, t2);
        return group;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
