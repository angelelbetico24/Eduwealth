package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.service.BrokerService;
import com.eduwealth.service.EducacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Navegadores;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Directorio de brokers (CU-09): búsqueda, filtros, recomendación según el nivel del usuario
 * y redirección a la web oficial tras un aviso explícito (RN-06).
 */
public class BrokersController {

    private static final int COLUMNAS = 2;

    private final BrokerService servicio = FabricaServicios.brokers();

    @FXML private Label saldoCabecera;
    @FXML private TextField campoBusqueda;
    @FXML private FlowPane filtros;
    @FXML private Label avisoError;
    @FXML private Label numeroBrokers;
    @FXML private GridPane rejillaBrokers;

    private final ToggleGroup grupoFiltros = new ToggleGroup();
    private List<Broker> brokers = List.of();
    private Nivel nivelUsuario = Nivel.PRINCIPIANTE;

    @FXML
    private void initialize() {
        for (int i = 0; i < COLUMNAS; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / COLUMNAS);
            rejillaBrokers.getColumnConstraints().add(c);
        }
        for (BrokerService.Filtro filtro : BrokerService.Filtro.values()) {
            ToggleButton boton = new ToggleButton(filtro.getEtiqueta());
            boton.setUserData(filtro);
            boton.setToggleGroup(grupoFiltros);
            boton.getStyleClass().add("filtro-nivel");
            boton.setSelected(filtro == BrokerService.Filtro.TODOS);
            filtros.getChildren().add(boton);
        }
        grupoFiltros.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                mostrarBrokers();
            }
        });
        campoBusqueda.textProperty().addListener((obs, a, b) -> mostrarBrokers());

        try {
            FabricaServicios.divisas().buscarPorId(Sesion.getUsuario().getIdDivisaBase()).ifPresent(d ->
                    saldoCabecera.setText("Saldo virtual:  "
                            + Formato.moneda(Sesion.getUsuario().getSaldoFicticio(), d.getSimbolo())));
            nivelUsuario = FabricaServicios.educacion().resumen(Sesion.getUsuario().getIdUsuario()).nivelActual();
            brokers = servicio.listar();
            mostrarBrokers();
        } catch (DataAccessException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarBrokers() {
        BrokerService.Filtro filtro = (BrokerService.Filtro) grupoFiltros.getSelectedToggle().getUserData();
        List<Broker> visibles = servicio.filtrar(brokers, campoBusqueda.getText(), filtro);
        numeroBrokers.setText(visibles.size() == 1 ? "1 broker" : visibles.size() + " brokers");
        rejillaBrokers.getChildren().clear();
        if (visibles.isEmpty()) {
            Label vacio = new Label("Ningún broker coincide con la búsqueda.");
            vacio.getStyleClass().add("nota");
            rejillaBrokers.add(vacio, 0, 0, COLUMNAS, 1);
            return;
        }
        for (int i = 0; i < visibles.size(); i++) {
            VBox tarjeta = tarjeta(visibles.get(i));
            rejillaBrokers.add(tarjeta, i % COLUMNAS, i / COLUMNAS);
        }
    }

    private VBox tarjeta(Broker b) {
        Label logo = new Label(MainController.calcularIniciales(b.getNombre()));
        logo.getStyleClass().add("broker-logo");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox cabecera = new HBox(10, logo, hueco);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        if (servicio.recomendadoPara(b, nivelUsuario)) {
            Label recomendado = new Label("★ Recomendado para tu nivel");
            recomendado.getStyleClass().add("chip-verde");
            cabecera.getChildren().add(recomendado);
        }

        Label nombre = new Label(b.getNombre());
        nombre.getStyleClass().add("leccion-titulo");

        FlowPane chips = new FlowPane(6, 6);
        Label regulado = new Label("✓ Regulado: " + b.getRegulacion());
        regulado.getStyleClass().add("chip-verde");
        chips.getChildren().add(regulado);
        for (TipoActivo tipo : b.getTiposActivo()) {
            Label chip = new Label(SimuladorController.nombreTipo(tipo));
            chip.getStyleClass().add("chip-gris");
            chips.getChildren().add(chip);
        }
        Label comision = new Label(textoComision(b.getComisionMinima()));
        comision.getStyleClass().add("chip");
        chips.getChildren().add(comision);

        Label descripcion = new Label(b.getDescripcion());
        descripcion.getStyleClass().add("subtitulo");
        descripcion.setWrapText(true);
        Label comisiones = new Label("Comisiones: " + b.getComisionesInfo());
        comisiones.getStyleClass().add("nota");
        comisiones.setWrapText(true);
        Label nivel = new Label("Nivel recomendado: " + EducacionService.nombreNivel(b.getNivelRecomendado()));
        nivel.getStyleClass().add("nota");

        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);
        Button abrir = new Button("Abrir cuenta real (enlace oficial)  ↗");
        abrir.getStyleClass().add("boton-primario");
        abrir.setMaxWidth(Double.MAX_VALUE);
        abrir.setOnAction(e -> confirmarRedireccion(b, abrir));

        VBox tarjeta = new VBox(10, cabecera, nombre, chips, descripcion, comisiones, nivel, empuje, abrir);
        tarjeta.getStyleClass().add("tarjeta-leccion");
        if (servicio.recomendadoPara(b, nivelUsuario)) {
            tarjeta.getStyleClass().add("tarjeta-leccion-siguiente");
        }
        tarjeta.setMaxHeight(Double.MAX_VALUE);
        return tarjeta;
    }

    /**
     * RN-06: antes de salir se avisa de que se abandona la simulación y se opera con dinero real
     * bajo la propia responsabilidad. Si se cancela, no ocurre nada (FA-01).
     */
    private void confirmarRedireccion(Broker broker, Button origen) {
        ocultarError();
        URI enlace;
        try {
            enlace = servicio.enlaceOficial(broker);
        } catch (ValidacionException e) {
            mostrarError(e.getMessage()); // E-01: enlace no disponible
            return;
        }
        ButtonType ir = new ButtonType("Ir a la web oficial", ButtonBar.ButtonData.OK_DONE);
        Alert aviso = new Alert(Alert.AlertType.WARNING, null, ir,
                new ButtonType("Seguir en EduWealth", ButtonBar.ButtonData.CANCEL_CLOSE));
        aviso.setTitle("Vas a salir de la simulación");
        aviso.setHeaderText("Abrir la web oficial de " + broker.getNombre());
        aviso.setContentText("A partir de aquí operarías con dinero real, bajo tu propia responsabilidad. "
                + "EduWealth no procesa ni almacena datos bancarios: cualquier alta o pago se hace directamente "
                + "en la web del broker.\n\nSe abrirá en tu navegador: " + enlace.getHost());
        aviso.initOwner(origen.getScene().getWindow());
        aviso.getDialogPane().getStylesheets().addAll(origen.getScene().getStylesheets());
        if (aviso.showAndWait().orElse(null) == ir) {
            try {
                Navegadores.abrir(enlace);
            } catch (RuntimeException e) {
                mostrarError("No se pudo abrir el navegador. Visita " + enlace + " manualmente.");
            }
        }
    }

    private static String textoComision(BigDecimal comision) {
        if (comision == null) {
            return "Comisión: consultar";
        }
        return comision.signum() == 0 ? "Sin comisión mínima" : "Desde " + Formato.moneda(comision, "€");
    }

    private void mostrarError(String texto) {
        avisoError.setText(texto);
        avisoError.setVisible(true);
        avisoError.setManaged(true);
    }

    private void ocultarError() {
        avisoError.setVisible(false);
        avisoError.setManaged(false);
    }
}
