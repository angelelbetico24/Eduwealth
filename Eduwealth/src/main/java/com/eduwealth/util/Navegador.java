package com.eduwealth.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Cambia la vista mostrada en la ventana principal. Las vistas son archivos
 * {@code /fxml/<nombre>.fxml} y todas comparten la hoja de estilos de la aplicación.
 */
public final class Navegador {

    public static final double ANCHO = 1280;
    public static final double ALTO = 800;
    private static final String HOJA_ESTILOS = "/css/eduwealth.css";

    private static Stage ventana;

    private Navegador() {
    }

    public static void inicializar(Stage stage) {
        ventana = stage;
    }

    /** Muestra la vista indicada y devuelve su controlador. */
    public static <T> T ir(String vista) {
        URL recurso = Objects.requireNonNull(
                Navegador.class.getResource("/fxml/" + vista + ".fxml"), "No existe la vista " + vista);
        FXMLLoader loader = new FXMLLoader(recurso);
        Parent raiz;
        try {
            raiz = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la vista " + vista, e);
        }

        Scene escena = ventana.getScene();
        if (escena == null) {
            escena = new Scene(raiz, ANCHO, ALTO);
            escena.getStylesheets().add(
                    Objects.requireNonNull(Navegador.class.getResource(HOJA_ESTILOS)).toExternalForm());
            ventana.setScene(escena);
        } else {
            escena.setRoot(raiz);
        }
        return loader.getController();
    }
}
