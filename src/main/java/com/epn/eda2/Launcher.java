package com.epn.eda2;

/**
 * Clase envoltorio (Launcher) que no extiende de javafx.application.Application.
 * Evita el error "JavaFX runtime components are missing" cuando se ejecuta directamente
 * desde un IDE (IntelliJ IDEA, VS Code, Eclipse) o desde un JAR empaquetado sin argumentos VM manuales.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
