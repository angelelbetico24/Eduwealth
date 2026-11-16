package com.eduwealth;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Punto de entrada JavaFX de EduWealth.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label bienvenida = new Label("EduWealth — en construcción");
        Scene scene = new Scene(new StackPane(bienvenida), 1280, 800);
        stage.setTitle("EduWealth");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
