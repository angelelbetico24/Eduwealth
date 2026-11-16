package com.eduwealth.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Carga vistas FXML ({@code /fxml/<nombre>.fxml}) y cambia la pantalla de la ventana principal.
 * Todas las vistas comparten la hoja de estilos de la aplicación.
 */
public final class Navegador {

    public static final double ANCHO = 1280;
    public static final double ALTO = 800;
    private static final String HOJA_ESTILOS = "/css/eduwealth.css";

    private static Stage ventana;

    /** Nodo raíz de una vista cargada y su controlador. */
    public record Vista<T>(Parent raiz, T controlador) {
    }

    private Navegador() {
    }

    public static void inicializar(Stage stage) {
        ventana = stage;
    }

    /** Carga una vista sin mostrarla (para insertarla dentro de otra, como el área de contenido). */
    public static <T> Vista<T> cargar(String vista) {
        URL recurso = Objects.requireNonNull(
                Navegador.class.getResource("/fxml/" + vista + ".fxml"), "No existe la vista " + vista);
        FXMLLoader loader = new FXMLLoader(recurso);
        try {
            Parent raiz = loader.load();
            return new Vista<>(raiz, loader.getController());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la vista " + vista, e);
        }
    }

    /** Muestra la vista indicada como pantalla completa de la ventana y devuelve su controlador. */
    public static <T> T ir(String vista) {
        Vista<T> cargada = cargar(vista);
        Scene escena = ventana.getScene();
        if (escena == null) {
            escena = new Scene(cargada.raiz(), ANCHO, ALTO);
            escena.getStylesheets().add(
                    Objects.requireNonNull(Navegador.class.getResource(HOJA_ESTILOS)).toExternalForm());
            ventana.setScene(escena);
        } else {
            escena.setRoot(cargada.raiz());
        }
        return cargada.controlador();
    }
}
