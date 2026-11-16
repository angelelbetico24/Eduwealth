package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.AutenticacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Navegador;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;

/**
 * Pantalla de registro de usuario (CU-01). Tras registrarse, el usuario entra directamente.
 */
public class RegistroController {

    private static final String ESTILO_ERROR = "campo-error";

    private final AutenticacionService autenticacion = FabricaServicios.autenticacion();

    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoEmail;
    @FXML
    private ComboBox<Divisa> comboDivisa;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private PasswordField campoConfirmacion;
    @FXML
    private Label mensajeError;

    /** Relaciona el nombre de campo de {@link ValidacionException} con su control. */
    private Map<String, Node> controlesPorCampo;

    @FXML
    private void initialize() {
        controlesPorCampo = Map.of(
                "nombre", campoNombre,
                "email", campoEmail,
                "divisa", comboDivisa,
                "password", campoPassword,
                "confirmacion", campoConfirmacion);
        try {
            List<Divisa> divisas = FabricaServicios.divisas().listarDivisas();
            comboDivisa.getItems().setAll(divisas);
            divisas.stream().filter(d -> "EUR".equals(d.getCodigo())).findFirst()
                    .ifPresent(comboDivisa::setValue);
        } catch (DataAccessException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void registrar() {
        limpiarErrores();
        try {
            Usuario usuario = autenticacion.registrar(
                    campoNombre.getText(),
                    campoEmail.getText(),
                    campoPassword.getText(),
                    campoConfirmacion.getText(),
                    comboDivisa.getValue());
            Sesion.iniciar(usuario);
            Navegador.ir("panel");
        } catch (ValidacionException e) {
            mostrarError(e.getMessage());
            Node control = e.getCampo() != null ? controlesPorCampo.get(e.getCampo()) : null;
            if (control != null) {
                control.getStyleClass().add(ESTILO_ERROR);
                control.requestFocus();
            }
        } catch (DataAccessException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void irALogin() {
        Navegador.ir("login");
    }

    private void mostrarError(String mensaje) {
        mensajeError.setText(mensaje);
        mensajeError.setVisible(true);
        mensajeError.setManaged(true);
    }

    private void limpiarErrores() {
        mensajeError.setVisible(false);
        mensajeError.setManaged(false);
        controlesPorCampo.values().forEach(c -> c.getStyleClass().remove(ESTILO_ERROR));
    }
}
