package com.epn.eda2;

import com.epn.eda2.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Clase Principal de la Aplicación JavaFX.
 * Estructuras de Datos y Algoritmos II — Proyecto 8 (Grupo 14).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1280, 780);

        URL cssUrl = MainApp.class.getResource("/styles/projector.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("EPN - EDA II | Proyecto 8 - Grupo 14: Sistema de Recomendación con Grafos Bipartitos (BFS)");
        primaryStage.setMinWidth(1040);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
