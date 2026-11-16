package com.eduwealth.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Marcador de un módulo pendiente de implementar. */
public class ProximamenteController {

    @FXML
    private Label titulo;
    @FXML
    private Label descripcion;

    public void mostrar(Seccion seccion) {
        titulo.setText(seccion.getTitulo());
        descripcion.setText(seccion.getDescripcion());
    }
}
