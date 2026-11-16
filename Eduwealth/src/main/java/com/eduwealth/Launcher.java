package com.eduwealth;

/**
 * Clase de arranque que no extiende de {@link javafx.application.Application}.
 * <p>
 * Permite ejecutar la aplicación desde el classpath (botón "Run" de VS Code o
 * un JAR) sin declarar módulos: si la clase principal extendiera de
 * Application, la JVM exigiría JavaFX en el module-path y fallaría con
 * "JavaFX runtime components are missing".
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
