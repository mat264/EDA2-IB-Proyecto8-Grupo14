package com.epn.eda2.ui;

import com.epn.eda2.algorithm.BFSRecommender;
import com.epn.eda2.metrics.BenchmarkResult;
import com.epn.eda2.metrics.Evaluator;
import com.epn.eda2.model.BipartiteGraph;
import com.epn.eda2.model.Movie;
import com.epn.eda2.model.Rating;
import com.epn.eda2.model.Recommendation;
import com.epn.eda2.model.User;
import com.epn.eda2.util.CsvDatasetLoader;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Vista Principal en JavaFX optimizada para proyección en Defensa Oral:
 * - Pestaña 1: Sistema de Recomendación y Métricas (Fuentes 14px-17px, wrapText dinámico sin truncamiento,
 *              Tooltips en cada fila y paleta cromática coherente).
 * - Pestaña 2: Visualizador Interactivo del Grafo Bipartito con animaciones suaves y lenguaje natural.
 */
public class MainView extends BorderPane {

    private static final String DATASET_SYNTHETIC = "Sintético Demo (6 usuarios, 8 películas)";
    private static final String DATASET_MOVIELENS = "MovieLens (data/movies.csv y data/ratings.csv)";

    private static final String CARD_STYLE =
            "-fx-background-color: #FFFFFF; "
            + "-fx-border-color: #CBD5E1; "
            + "-fx-border-radius: 8px; "
            + "-fx-background-radius: 8px; "
            + "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.07), 10, 0, 0, 2);";

    private final BipartiteGraph graph;
    private List<Movie> currentMovies;
    private List<Rating> currentRatings;
    private double lastGraphBuildMs = 0.0;

    private ComboBox<String> datasetComboBox;
    private ComboBox<User> userComboBox;
    private Spinner<Integer> kSpinner;

    private TableView<WatchedMovieRow> historyTable;
    private TableView<RecommendationRow> recommendationTable;

    // Etiquetas del panel lateral de métricas y estadísticas
    private Label graphStatsLabel;
    private Label precisionLabel;
    private Label recallLabel;
    private Label buildTimeLabel;
    private Label avgRecTimeLabel;
    private Label memoryUsedLabel;
    private Label evalInfoLabel;

    private GraphVisualizerPane visualizerPane;

    public MainView() {
        this.graph = new BipartiteGraph();
        this.currentMovies = new ArrayList<>();
        this.currentRatings = new ArrayList<>();

        setStyle("-fx-background-color: #F8FAFC; -fx-font-size: 14px;");
        setTop(createHeaderBanner());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tab1 = new Tab("1. Sistema de Recomendación y Métricas", createRecommendationTabContent());
        visualizerPane = new GraphVisualizerPane(graph);
        Tab tab2 = new Tab("2. Visualizador Interactivo del Grafo (Defensa Oral)", visualizerPane);

        tabPane.getTabs().addAll(tab1, tab2);
        setCenter(tabPane);

        // Cargar por defecto el dataset Sintético Demo embebido en resources
        loadEmbeddedDataset(DATASET_SYNTHETIC);
    }

    private HBox createHeaderBanner() {
        Label title = new Label("EPN — Estructuras de Datos y Algoritmos II | Proyecto 8 (Grupo 14)");
        title.setFont(Font.font("System", FontWeight.BOLD, 17));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Recomendador en Grafos Bipartitos mediante Búsqueda en Anchura (BFS)");
        subtitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        subtitle.setTextFill(Color.web("#BAE6FD"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox banner = new HBox(14, title, spacer, subtitle);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(12, 18, 12, 18));
        banner.setStyle("-fx-background-color: #0F172A;");
        return banner;
    }

    private BorderPane createRecommendationTabContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #F8FAFC;");

        // Barra superior de configuración (Selector Dataset + FileChooser + Usuario + K)
        HBox topToolbar = createTopControlBar();
        root.setTop(topToolbar);

        // Panel central con las 2 tablas (Películas Vistas y Recomendaciones Top-K)
        VBox tablesContainer = createCenterTablesBox();
        BorderPane.setMargin(tablesContainer, new Insets(12, 12, 0, 0));
        root.setCenter(tablesContainer);

        // Panel lateral derecho con Benchmarks y Métricas
        VBox benchmarkSidePanel = createBenchmarkSidePanel();
        BorderPane.setMargin(benchmarkSidePanel, new Insets(12, 0, 0, 0));
        root.setRight(benchmarkSidePanel);

        return root;
    }

    private HBox createTopControlBar() {
        Label dsLabel = new Label("Dataset:");
        dsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        datasetComboBox = new ComboBox<>();
        datasetComboBox.getItems().addAll(DATASET_SYNTHETIC, DATASET_MOVIELENS);
        datasetComboBox.setValue(DATASET_SYNTHETIC);
        datasetComboBox.setPrefWidth(295);
        datasetComboBox.setStyle("-fx-font-size: 14px;");
        datasetComboBox.setOnAction(e -> loadEmbeddedDataset(datasetComboBox.getValue()));

        Button loadExternalCsvBtn = new Button("📂 Cargar CSV Externos...");
        loadExternalCsvBtn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 14;");
        loadExternalCsvBtn.setOnAction(e -> handleLoadExternalCsv());

        Label userLabel = new Label("Usuario Objetivo:");
        userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        userComboBox = new ComboBox<>();
        userComboBox.setPrefWidth(175);
        userComboBox.setStyle("-fx-font-size: 14px;");
        // Listener automático al cambiar de Usuario (u) sin requerir clic adicional
        userComboBox.valueProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                refreshRecommendationsForSelectedUser();
            }
        });

        Label kLabel = new Label("Top-K:");
        kLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        SpinnerValueFactory.IntegerSpinnerValueFactory kFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5);
        kSpinner = new Spinner<>(kFactory);
        kSpinner.setEditable(true);
        kSpinner.setPrefWidth(78);
        kSpinner.setStyle("-fx-font-size: 14px;");
        // Listener automático al cambiar el valor de Top-K con las flechas o teclado
        kSpinner.valueProperty().addListener((obs, oldK, newK) -> {
            if (newK != null) {
                refreshRecommendationsForSelectedUser();
            }
        });
        kSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.matches("\\d+")) {
                try {
                    int parsed = Integer.parseInt(newText);
                    if (parsed >= kFactory.getMin() && parsed <= kFactory.getMax()
                            && !Integer.valueOf(parsed).equals(kSpinner.getValue())) {
                        kFactory.setValue(parsed);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });

        Button recommendBtn = new Button("Recomendar (BFS)");
        recommendBtn.setStyle(
                "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 16;");
        recommendBtn.setOnAction(e -> refreshRecommendationsForSelectedUser());

        HBox bar = new HBox(12,
                dsLabel, datasetComboBox,
                loadExternalCsvBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                userLabel, userComboBox,
                kLabel, kSpinner,
                recommendBtn
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle(CARD_STYLE);
        return bar;
    }

    private VBox createCenterTablesBox() {
        // Encabezado Sección 1: Películas Vistas (Verde Esmeralda #0D9488)
        Label histTitle = new Label("● Películas Vistas por el Usuario Objetivo (Nivel 1 — Calificación ≥ 4.0)");
        histTitle.getStyleClass().add("section-header");
        histTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0D9488;");

        historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPrefHeight(205);
        historyTable.setStyle("-fx-font-size: 14px;");

        TableColumn<WatchedMovieRow, Integer> colHistId = new TableColumn<>("ID");
        colHistId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().movieId()));
        colHistId.setMinWidth(65);
        colHistId.setMaxWidth(80);

        TableColumn<WatchedMovieRow, String> colHistTitle = new TableColumn<>("Película Vista");
        colHistTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        colHistTitle.setMinWidth(220);

        TableColumn<WatchedMovieRow, String> colHistGenres = new TableColumn<>("Géneros");
        colHistGenres.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().genres()));
        colHistGenres.setMinWidth(180);

        TableColumn<WatchedMovieRow, String> colHistRating = new TableColumn<>("Calificación");
        colHistRating.setCellValueFactory(data -> new SimpleStringProperty(
                String.format(Locale.US, "★ %.1f", data.getValue().rating())));
        colHistRating.setMinWidth(105);
        colHistRating.setMaxWidth(120);

        historyTable.getColumns().addAll(List.of(colHistId, colHistTitle, colHistGenres, colHistRating));

        // Tooltip legible al pasar el cursor sobre cualquier fila del historial
        historyTable.setRowFactory(tv -> new TableRow<>() {
            private final Tooltip rowTooltip = new Tooltip();
            {
                rowTooltip.setStyle("-fx-font-size: 14px;");
            }
            @Override
            protected void updateItem(WatchedMovieRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setTooltip(null);
                } else {
                    rowTooltip.setText(String.format(
                            Locale.US,
                            "Película Vista [ID %d]: %s\nGéneros: %s\nCalificación otorgada: ★ %.1f",
                            item.movieId(), item.title(), item.genres(), item.rating()
                    ));
                    setTooltip(rowTooltip);
                }
            }
        });

        // Encabezado Sección 2: Recomendaciones Finales (Naranja #EA580C)
        Label recTitle = new Label("● Películas Candidatas Recomendadas — Top-K (Nivel 3)");
        recTitle.getStyleClass().add("section-header");
        recTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #EA580C;");

        recommendationTable = new TableView<>();
        recommendationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recommendationTable.setStyle("-fx-font-size: 14px;");
        VBox.setVgrow(recommendationTable, Priority.ALWAYS);

        TableColumn<RecommendationRow, Integer> colRank = new TableColumn<>("#");
        colRank.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().rank()));
        colRank.setMinWidth(42);
        colRank.setMaxWidth(50);

        TableColumn<RecommendationRow, Integer> colRecId = new TableColumn<>("ID");
        colRecId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().movieId()));
        colRecId.setMinWidth(55);
        colRecId.setMaxWidth(65);

        TableColumn<RecommendationRow, String> colRecTitle = new TableColumn<>("Película Candidata");
        colRecTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        colRecTitle.setMinWidth(185);
        colRecTitle.setPrefWidth(215);

        TableColumn<RecommendationRow, Integer> colPaths = new TableColumn<>("Caminos");
        colPaths.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().paths()));
        colPaths.setMinWidth(80);
        colPaths.setMaxWidth(95);

        TableColumn<RecommendationRow, String> colScore = new TableColumn<>("Puntaje");
        colScore.setCellValueFactory(data -> new SimpleStringProperty(
                String.format(Locale.US, "%.2f", data.getValue().score())));
        colScore.setMinWidth(80);
        colScore.setMaxWidth(95);

        TableColumn<RecommendationRow, String> colExplanation =
                new TableColumn<>("Explicación / Trazabilidad (Películas Vistas y Usuarios Similares)");
        colExplanation.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().explanation()));
        colExplanation.setMinWidth(320);
        colExplanation.setPrefWidth(460);

        // Celda personalizada con wrapText y altura dinámica para que nunca se corte con "..."
        colExplanation.setCellFactory(col -> new TableCell<>() {
            private final Label wrapLabel = new Label();
            {
                wrapLabel.setWrapText(true);
                wrapLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B; -fx-padding: 4 2 4 2;");
                wrapLabel.setMaxWidth(Double.MAX_VALUE);
                wrapLabel.setPrefHeight(Control.USE_COMPUTED_SIZE);
                wrapLabel.prefWidthProperty().bind(col.widthProperty().subtract(20));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    wrapLabel.setText(item);
                    setGraphic(wrapLabel);
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                }
            }
        });

        recommendationTable.getColumns().addAll(
                List.of(colRank, colRecId, colRecTitle, colPaths, colScore, colExplanation));

        // Tooltip legible al pasar el cursor sobre cualquier fila de recomendaciones
        recommendationTable.setRowFactory(tv -> new TableRow<>() {
            private final Tooltip rowTooltip = new Tooltip();
            {
                rowTooltip.setWrapText(true);
                rowTooltip.setMaxWidth(520);
                rowTooltip.setStyle("-fx-font-size: 14px;");
            }
            @Override
            protected void updateItem(RecommendationRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setTooltip(null);
                } else {
                    rowTooltip.setText(String.format(
                            Locale.US,
                            "Top #%d — %s [ID %d]\nPuntaje Total: %.2f (%d caminos encontrados)\n%s",
                            item.rank(), item.title(), item.movieId(),
                            item.score(), item.paths(), item.explanation()
                    ));
                    setTooltip(rowTooltip);
                }
            }
        });

        VBox box = new VBox(10, histTitle, historyTable, recTitle, recommendationTable);
        return box;
    }

    private VBox createBenchmarkSidePanel() {
        Label title = new Label("📊 Evaluación y Benchmarks");
        title.getStyleClass().add("section-header");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label sub = new Label("Validación Experimental Train/Test (80/20)");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        graphStatsLabel = new Label("Grafo: |U|=0, |V|=0, |E|=0");
        graphStatsLabel.setWrapText(true);
        graphStatsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        Button runBenchmarkBtn = new Button("⚡ Ejecutar Benchmark (80/20)");
        runBenchmarkBtn.setMaxWidth(Double.MAX_VALUE);
        runBenchmarkBtn.setStyle(
                "-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 10 14;");
        runBenchmarkBtn.setOnAction(e -> runAutomatedBenchmarks());

        precisionLabel = createMetricValueLabel("Precision@K: —");
        recallLabel = createMetricValueLabel("Recall@K: —");
        buildTimeLabel = createMetricValueLabel("Construcción grafo: — ms");
        avgRecTimeLabel = createMetricValueLabel("Tiempo prom. rec.: — ms");
        memoryUsedLabel = createMetricValueLabel("Memoria utilizada: — MB");

        evalInfoLabel = new Label("Presione 'Ejecutar Benchmark' para evaluar el modelo con 80% Entrenamiento y 20% Prueba.");
        evalInfoLabel.setWrapText(true);
        evalInfoLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #334155;");

        // Leyenda de colores coherente entre Pestaña 1 y Pestaña 2
        VBox legendBox = createColorPaletteLegend();

        VBox metricsCard = new VBox(11,
                title,
                sub,
                new Separator(),
                graphStatsLabel,
                runBenchmarkBtn,
                new Separator(),
                precisionLabel,
                recallLabel,
                buildTimeLabel,
                avgRecTimeLabel,
                memoryUsedLabel,
                new Separator(),
                evalInfoLabel,
                new Separator(),
                legendBox
        );
        metricsCard.setPadding(new Insets(16));
        metricsCard.setPrefWidth(325);
        metricsCard.setStyle(CARD_STYLE);
        return metricsCard;
    }

    private VBox createColorPaletteLegend() {
        Label legendTitle = new Label("Código de Colores (Grafo BFS):");
        legendTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label l0 = new Label("● Azul (#2563EB): Usuario Objetivo");
        l0.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        Label l1 = new Label("● Verde (#0D9488): Películas Vistas");
        l1.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0D9488;");

        Label l2 = new Label("● Morado (#7C3AED): Usuarios Similares");
        l2.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #7C3AED;");

        Label l3 = new Label("● Naranja (#EA580C): Recomendaciones");
        l3.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #EA580C;");

        return new VBox(4, legendTitle, l0, l1, l2, l3);
    }

    private Label createMetricValueLabel(String initialText) {
        Label lbl = new Label(initialText);
        lbl.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void loadEmbeddedDataset(String datasetOption) {
        try {
            if (DATASET_SYNTHETIC.equals(datasetOption)) {
                currentMovies = CsvDatasetLoader.loadMoviesPortable(
                        null, CsvDatasetLoader.SYNTHETIC_MOVIES_RESOURCE);
                currentRatings = CsvDatasetLoader.loadRatingsPortable(
                        null, CsvDatasetLoader.SYNTHETIC_RATINGS_RESOURCE);
            } else {
                currentMovies = CsvDatasetLoader.loadMoviesPortable(
                        CsvDatasetLoader.RELATIVE_MOVIES_PATH, CsvDatasetLoader.MOVIELENS_MOVIES_RESOURCE);
                currentRatings = CsvDatasetLoader.loadRatingsPortable(
                        CsvDatasetLoader.RELATIVE_RATINGS_PATH, CsvDatasetLoader.MOVIELENS_RATINGS_RESOURCE);
            }
            rebuildGraphAndRefreshUI();
        } catch (Exception ex) {
            showErrorAlert("Error al cargar dataset", ex.getMessage());
        }
    }

    private void handleLoadExternalCsv() {
        Window window = getScene() != null ? getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));

        chooser.setTitle("1/2: Seleccione el archivo movies.csv (movieId,title,genres)");
        File moviesFile = chooser.showOpenDialog(window);
        if (moviesFile == null) {
            return;
        }

        chooser.setInitialDirectory(moviesFile.getParentFile());
        chooser.setTitle("2/2: Seleccione el archivo ratings.csv (userId,movieId,rating)");
        File ratingsFile = chooser.showOpenDialog(window);
        if (ratingsFile == null) {
            return;
        }

        try {
            currentMovies = CsvDatasetLoader.loadMoviesFromFile(moviesFile);
            currentRatings = CsvDatasetLoader.loadRatingsFromFile(ratingsFile);
            rebuildGraphAndRefreshUI();
        } catch (Exception ex) {
            showErrorAlert("Error al leer CSV externos", ex.getMessage());
        }
    }

    private void rebuildGraphAndRefreshUI() {
        long t0 = System.nanoTime();
        graph.buildFromDataset(currentMovies, currentRatings, BipartiteGraph.DEFAULT_POSITIVE_RATING_THRESHOLD);
        long t1 = System.nanoTime();
        lastGraphBuildMs = (t1 - t0) / 1_000_000.0;

        graphStatsLabel.setText(String.format(
                Locale.US,
                "Grafo Bipartito Activo:\n• Usuarios: %d\n• Películas en catálogo: %d\n• Interacciones (rating ≥ 4.0): %d",
                graph.getUserCount(), graph.getMovieCount(), graph.getEdgeCount()
        ));

        List<User> users = new ArrayList<>();
        for (Integer uid : graph.getAllUserIds()) {
            users.add(new User(uid));
        }
        Collections.sort(users);

        userComboBox.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            userComboBox.setValue(users.get(0));
        }

        refreshRecommendationsForSelectedUser();
        runAutomatedBenchmarks();
    }

    private void refreshRecommendationsForSelectedUser() {
        User selectedUser = userComboBox.getValue();
        if (selectedUser == null) {
            historyTable.getItems().clear();
            recommendationTable.getItems().clear();
            return;
        }

        int userId = selectedUser.getUserId();
        int k = kSpinner.getValue() != null ? kSpinner.getValue() : 5;

        // 1. Llenar tabla de Películas Vistas (Nivel 1)
        Set<Integer> watchedIds = graph.getMoviesWatchedByUser(userId);
        ObservableList<WatchedMovieRow> historyRows = FXCollections.observableArrayList();
        watchedIds.stream().sorted().forEach(movieId -> {
            Movie m = graph.getMovie(movieId);
            double rating = graph.getRating(userId, movieId);
            historyRows.add(new WatchedMovieRow(m.getMovieId(), m.getTitle(), m.getGenres(), rating));
        });
        historyTable.setItems(historyRows);

        // 2. Ejecutar algoritmo BFS y llenar tabla de Películas Candidatas Recomendadas (Nivel 3)
        BFSRecommender recommender = new BFSRecommender(graph);
        List<Recommendation> topK = recommender.recommendTopK(userId, k);

        ObservableList<RecommendationRow> recRows = FXCollections.observableArrayList();
        int rank = 1;
        for (Recommendation rec : topK) {
            recRows.add(new RecommendationRow(
                    rank++,
                    rec.getMovieId(),
                    rec.getTitle() + " (" + rec.getGenres() + ")",
                    rec.getPathCount(),
                    rec.getScore(),
                    rec.getExplanation()
            ));
        }
        recommendationTable.setItems(recRows);

        // 3. Sincronizar la Pestaña 2 (Visualizador Interactivo) con el usuario y el Top-K actual,
        //    reiniciando el visualizador al paso inicial para que el Nivel 3 refleje exactamente K recomendaciones
        if (visualizerPane != null) {
            visualizerPane.updateGraphUserAndK(graph, userId, k);
        }
    }

    private void runAutomatedBenchmarks() {
        int k = kSpinner.getValue() != null ? kSpinner.getValue() : 5;
        BenchmarkResult result = Evaluator.evaluateTrainTest80_20(
                currentMovies,
                currentRatings,
                BipartiteGraph.DEFAULT_POSITIVE_RATING_THRESHOLD,
                k
        );

        precisionLabel.setText(String.format(Locale.US, "Precision@%d:      %.4f (%.1f%%)",
                result.getK(), result.getPrecisionAtK(), result.getPrecisionAtK() * 100.0));
        recallLabel.setText(String.format(Locale.US, "Recall@%d:         %.4f (%.1f%%)",
                result.getK(), result.getRecallAtK(), result.getRecallAtK() * 100.0));
        buildTimeLabel.setText(String.format(Locale.US, "Construcción Grafo: %.3f ms",
                result.getGraphBuildTimeMs()));
        avgRecTimeLabel.setText(String.format(Locale.US, "Tiempo Prom. Rec.:  %.4f ms/usu.",
                result.getAvgRecommendationTimeMs()));
        memoryUsedLabel.setText(String.format(Locale.US, "Memoria Utilizada:  %.2f MB",
                result.getMemoryUsedMb()));

        evalInfoLabel.setText(String.format(
                Locale.US,
                "Partición 80/20 completada:\n• Usuarios evaluados: %d\n• Datos Entrenamiento (80%%): %d\n• Datos Prueba (20%%): %d",
                result.getEvaluatedUsers(), result.getTrainEdges(), result.getTestEdges()
        ));
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public record WatchedMovieRow(int movieId, String title, String genres, double rating) {}

    public record RecommendationRow(int rank, int movieId, String title, int paths, double score, String explanation) {}
}
