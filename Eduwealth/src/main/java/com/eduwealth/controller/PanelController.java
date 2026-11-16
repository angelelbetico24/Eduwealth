package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Navegador;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Panel provisional tras el inicio de sesión. Muestra los datos del usuario
 * autenticado; se sustituirá por el Panel Principal con menú lateral.
 */
public class PanelController {

    @FXML
    private Label saludo;
    @FXML
    private Label rol;
    @FXML
    private Label email;
    @FXML
    private Label saldo;

    @FXML
    private void initialize() {
        Usuario usuario = Sesion.getUsuario();
        saludo.setText("¡Hola, " + usuario.getNombre() + "!");
        rol.setText(usuario.esAdmin() ? "Administrador" : "Estudiante");
        email.setText(usuario.getEmail());

        String simbolo;
        try {
            simbolo = FabricaServicios.divisas().buscarPorId(usuario.getIdDivisaBase())
                    .map(d -> d.getSimbolo()).orElse("");
        } catch (DataAccessException e) {
            simbolo = "";
        }
        saldo.setText(Formato.moneda(usuario.getSaldoFicticio(), simbolo));
    }

    @FXML
    private void cerrarSesion() {
        Sesion.cerrar();
        Navegador.ir("login");
    }
}
