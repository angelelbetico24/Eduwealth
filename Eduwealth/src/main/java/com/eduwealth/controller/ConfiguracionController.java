package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.AutenticacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.PerfilService;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuración del perfil (RF-03) y reinicio del simulador (RF-19, CU-13). Las reglas y las
 * transacciones están en {@link PerfilService}.
 */
public class ConfiguracionController {

    private static final String ESTILO_ERROR = "campo-error";

    private final PerfilService perfil = FabricaServicios.perfil();

    @FXML private Label saldoCabecera;
    @FXML private TextField campoNombre;
    @FXML private TextField campoEmail;
    @FXML private ComboBox<Divisa> comboDivisa;
    @FXML private Label notaDivisa;
    @FXML private Label mensajePerfil;
    @FXML private PasswordField campoActual;
    @FXML private PasswordField campoNueva;
    @FXML private PasswordField campoConfirmacion;
    @FXML private Label mensajePassword;
    @FXML private Label mensajeSimulador;
    @FXML private Button botonReiniciar;

    private List<Divisa> divisas = List.of();
    private Runnable alCambiarPerfil = () -> { };

    @FXML
    private void initialize() {
        Usuario u = Sesion.getUsuario();
        campoNombre.setText(u.getNombre());
        campoEmail.setText(u.getEmail());
        try {
            divisas = FabricaServicios.divisas().listarDivisas();
            comboDivisa.getItems().setAll(divisas);
            comboDivisa.setValue(divisaActual().orElse(null));
        } catch (DataAccessException e) {
            mostrar(mensajePerfil, e.getMessage(), false);
        }
        comboDivisa.valueProperty().addListener((obs, a, b) -> actualizarNotaDivisa());
        actualizarNotaDivisa();
        actualizarSaldo();
    }

    /** El MainController refresca el menú lateral (nombre y divisa) tras un cambio. */
    public void setAlCambiarPerfil(Runnable alCambiarPerfil) {
        this.alCambiarPerfil = alCambiarPerfil;
    }

    // ------------------------------------------------------------------ perfil y divisa (RF-03)

    @FXML
    private void guardarPerfil() {
        ocultar(mensajePerfil);
        campoNombre.getStyleClass().remove(ESTILO_ERROR);
        Usuario u = Sesion.getUsuario();
        Divisa nueva = comboDivisa.getValue();
        boolean cambiaDivisa = nueva != null && nueva.getIdDivisa() != u.getIdDivisaBase();
        if (cambiaDivisa && !confirmarCambioDivisa(nueva)) {
            return;
        }
        try {
            u.setNombre(perfil.cambiarNombre(u.getIdUsuario(), campoNombre.getText()));
            String texto = "Perfil guardado.";
            if (cambiaDivisa) {
                Usuario actualizado = perfil.cambiarDivisaBase(u.getIdUsuario(), nueva);
                u.setIdDivisaBase(actualizado.getIdDivisaBase());
                u.setSaldoFicticio(actualizado.getSaldoFicticio());
                u.setCapitalInicial(actualizado.getCapitalInicial());
                texto = "Perfil guardado. Tu saldo, tus presupuestos y tus gastos ahora están en " + nueva.getCodigo() + ".";
            }
            mostrar(mensajePerfil, texto, true);
            actualizarSaldo();
            actualizarNotaDivisa();
            alCambiarPerfil.run();
        } catch (ValidacionException e) {
            mostrar(mensajePerfil, e.getMessage(), false);
            if ("nombre".equals(e.getCampo())) {
                campoNombre.getStyleClass().add(ESTILO_ERROR);
            }
        } catch (DataAccessException e) {
            mostrar(mensajePerfil, e.getMessage(), false); // la transacción se ha deshecho
        }
    }

    private boolean confirmarCambioDivisa(Divisa nueva) {
        Divisa actual = divisaActual().orElse(nueva);
        BigDecimal unidad = FabricaServicios.divisas().convertir(BigDecimal.ONE, actual, nueva)
                .round(new MathContext(5));
        return confirmar("Cambiar la divisa base",
                "¿Pasar de " + actual.getCodigo() + " a " + nueva.getCodigo() + "?",
                "Al tipo de cambio actual (1 " + actual.getCodigo() + " = " + unidad.toPlainString().replace('.', ',')
                        + " " + nueva.getCodigo() + ") se convertirán tu saldo virtual, tus límites de presupuesto "
                        + "y tus gastos. Tus posiciones siguen en la divisa de cada activo.",
                "Cambiar divisa");
    }

    private void actualizarNotaDivisa() {
        Divisa elegida = comboDivisa.getValue();
        boolean cambia = elegida != null && elegida.getIdDivisa() != Sesion.getUsuario().getIdDivisaBase();
        notaDivisa.setText(cambia
                ? "Al guardar se convertirán tu saldo, tus presupuestos y tus gastos a " + elegida.getCodigo() + " (RN-04)."
                : "Toda tu cartera y tus presupuestos se consolidan automáticamente en esta divisa (RN-04).");
    }

    // ------------------------------------------------------------------ contraseña (RF-03, RNF-01)

    @FXML
    private void cambiarPassword() {
        ocultar(mensajePassword);
        Map<String, Node> campos = Map.of("actual", campoActual, "nueva", campoNueva, "confirmacion", campoConfirmacion);
        campos.values().forEach(c -> c.getStyleClass().remove(ESTILO_ERROR));
        try {
            perfil.cambiarPassword(Sesion.getUsuario().getIdUsuario(), campoActual.getText(), campoNueva.getText(),
                    campoConfirmacion.getText());
            campoActual.clear();
            campoNueva.clear();
            campoConfirmacion.clear();
            mostrar(mensajePassword, "Contraseña actualizada.", true);
        } catch (ValidacionException e) {
            mostrar(mensajePassword, e.getMessage(), false);
            Node campo = e.getCampo() == null ? null : campos.get(e.getCampo());
            if (campo != null) {
                campo.getStyleClass().add(ESTILO_ERROR);
                campo.requestFocus();
            }
        } catch (DataAccessException e) {
            mostrar(mensajePassword, e.getMessage(), false);
        }
    }

    // ------------------------------------------------------------------ reinicio del simulador (RF-19, CU-13)

    @FXML
    private void reiniciarSimulador() {
        ocultar(mensajeSimulador);
        String inicial = Formato.moneda(AutenticacionService.SALDO_INICIAL, simbolo());
        if (!confirmar("Reiniciar el simulador", "¿Empezar de cero en el simulador?",
                "Se borrarán tu cartera y tu historial de operaciones y tu saldo volverá a " + inicial
                        + ". Esta acción no se puede deshacer. Tu progreso educativo y tus presupuestos se conservan.",
                "Reiniciar")) {
            return;
        }
        try {
            BigDecimal saldo = perfil.reiniciarSimulador(Sesion.getUsuario().getIdUsuario());
            Sesion.getUsuario().setSaldoFicticio(saldo);
            Sesion.getUsuario().setCapitalInicial(saldo);
            actualizarSaldo();
            mostrar(mensajeSimulador, "Simulador reiniciado: tu saldo virtual vuelve a ser " + inicial + ".", true);
        } catch (ValidacionException | DataAccessException e) {
            mostrar(mensajeSimulador, e.getMessage(), false);
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private boolean confirmar(String titulo, String cabecera, String texto, String accion) {
        ButtonType aceptar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, aceptar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecera);
        alerta.initOwner(campoNombre.getScene().getWindow());
        alerta.getDialogPane().getStylesheets().addAll(campoNombre.getScene().getStylesheets());
        return alerta.showAndWait().orElse(null) == aceptar;
    }

    private void actualizarSaldo() {
        String simbolo = simbolo();
        saldoCabecera.setText("Saldo virtual:  " + Formato.moneda(Sesion.getUsuario().getSaldoFicticio(), simbolo));
        botonReiniciar.setText("Reiniciar saldo virtual a " + Formato.moneda(AutenticacionService.SALDO_INICIAL, simbolo));
    }

    private Optional<Divisa> divisaActual() {
        return divisas.stream().filter(d -> d.getIdDivisa() == Sesion.getUsuario().getIdDivisaBase()).findFirst();
    }

    private String simbolo() {
        return divisaActual().map(Divisa::getSimbolo).orElse("€");
    }

    private static void mostrar(Label etiqueta, String texto, boolean exito) {
        etiqueta.setText(texto);
        etiqueta.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        etiqueta.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
    }

    private static void ocultar(Label etiqueta) {
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
    }
}
