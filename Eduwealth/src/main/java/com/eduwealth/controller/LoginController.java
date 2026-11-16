package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.AutenticacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Navegador;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Pantalla de inicio de sesión (CU-02).
 */
public class LoginController {

    private final AutenticacionService autenticacion = FabricaServicios.autenticacion();

    @FXML
    private TextField campoEmail;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private Label mensajeError;

    @FXML
    private void entrar() {
        ocultarError();
        try {
            Usuario usuario = autenticacion.iniciarSesion(campoEmail.getText(), campoPassword.getText());
            Sesion.iniciar(usuario);
            Navegador.ir("main");
        } catch (ValidacionException e) {
            mostrarError(e.getMessage());
            campoPassword.clear();
        } catch (DataAccessException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void irARegistro() {
        Navegador.ir("registro");
    }

    private void mostrarError(String mensaje) {
        mensajeError.setText(mensaje);
        mensajeError.setVisible(true);
        mensajeError.setManaged(true);
    }

    private void ocultarError() {
        mensajeError.setVisible(false);
        mensajeError.setManaged(false);
    }
}
