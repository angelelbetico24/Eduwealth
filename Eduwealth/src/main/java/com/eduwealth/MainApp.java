package com.eduwealth;

import com.eduwealth.util.Navegador;
import com.eduwealth.util.Navegadores;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Punto de entrada JavaFX de EduWealth: abre la ventana con la pantalla de inicio de sesión.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Navegador.inicializar(stage);
        Navegadores.inicializar(getHostServices());
        Navegador.ir("login");
        stage.setTitle("EduWealth");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
