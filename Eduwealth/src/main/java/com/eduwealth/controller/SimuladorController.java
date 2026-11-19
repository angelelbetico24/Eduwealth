package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.TipoOperacion;
import com.eduwealth.service.Cotizacion;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.OperacionDetalle;
import com.eduwealth.service.PosicionDetalle;
import com.eduwealth.service.ResultadoOperacion;
import com.eduwealth.service.ResumenCartera;
import com.eduwealth.service.SimuladorService;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pantalla del simulador de inversiones. Solo gestiona la interfaz: las reglas
 * (RN-01, RN-02, RN-04) y las operaciones están en {@link SimuladorService}.
 */
public class SimuladorController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ESTILO_ERROR = "campo-error";
    private static final double ALTO_FILA = 52;

    private final SimuladorService simulador = FabricaServicios.simulador();

    @FXML private Label saldoCabecera;
    @FXML private Label avisoError;
    @FXML private Label valorCartera;
    @FXML private Label numeroPosiciones;
    @FXML private Label pnlNoRealizado;
    @FXML private Label patrimonio;
    @FXML private Label rentabilidadPct;
    @FXML private Label avisoDivisas;
    @FXML private TableView<PosicionDetalle> tablaPosiciones;
    @FXML private Button botonVender;
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<Opcion<TipoActivo>> filtroTipo;
    @FXML private ComboBox<Opcion<Divisa>> filtroDivisa;
    @FXML private TableView<Activo> tablaCatalogo;
    @FXML private ToggleGroup grupoTipo;
    @FXML private ToggleButton toggleCompra;
    @FXML private ToggleButton toggleVenta;
    @FXML private Label ordenTicker;
    @FXML private Label ordenNombre;
    @FXML private Label ordenPrecio;
    @FXML private Label ordenPoseidos;
    @FXML private TextField campoCantidad;
    @FXML private Button botonMaximo;
    @FXML private GridPane desglose;
    @FXML private Label mensajeOrden;
    @FXML private Button botonConfirmar;
    @FXML private TableView<OperacionDetalle> tablaHistorial;

    private List<Activo> catalogo = List.of();
    private Map<Integer, Divisa> divisas = Map.of();
    private ResumenCartera resumen;
    private Activo activoSeleccionado;

    /** Elemento de un desplegable con texto visible y valor (null = «todos»). */
    private record Opcion<T>(String etiqueta, T valor) {
        @Override
        public String toString() {
            return etiqueta;
        }
    }

    @FXML
    private void initialize() {
        configurarTablaPosiciones();
        configurarTablaCatalogo();
        configurarTablaHistorial();

        // Un grupo de dos opciones nunca debe quedar sin selección
        grupoTipo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                actualizarModo();
            }
        });
        campoCantidad.textProperty().addListener((obs, a, b) -> recalcular()); // FA-01

        try {
            divisas = simulador.divisasPorId();
            catalogo = simulador.catalogo();
            configurarFiltros();
            recargarCartera();
        } catch (DataAccessException e) {
            avisoError.setText(e.getMessage());
            avisoError.setVisible(true);
            avisoError.setManaged(true);
        }
        actualizarModo();
        mostrarActivo();
    }

    // ------------------------------------------------------------------ carga de datos

    private void recargarCartera() {
        resumen = simulador.resumenCartera(Sesion.getUsuario(), LocalDateTime.now());
        String simbolo = resumen.divisaBase().getSimbolo();

        saldoCabecera.setText("Saldo virtual:  " + Formato.moneda(resumen.saldoDisponible(), simbolo));
        valorCartera.setText(Formato.moneda(resumen.valorCartera(), simbolo));
        int n = resumen.posiciones().size();
        numeroPosiciones.setText(n == 1 ? "1 posición abierta" : n + " posiciones abiertas");

        boolean pnlPositivo = resumen.pnlNoRealizado().signum() >= 0;
        pnlNoRealizado.setText((pnlPositivo ? "+" : "") + Formato.moneda(resumen.pnlNoRealizado(), simbolo));
        estilo(pnlNoRealizado, pnlPositivo ? "texto-positivo" : "texto-negativo", "texto-positivo", "texto-negativo");

        patrimonio.setText(Formato.moneda(resumen.patrimonioTotal(), simbolo));
        rentabilidadPct.setText(Formato.porcentaje(resumen.rentabilidadPorcentaje(), true));
        estilo(rentabilidadPct, estiloRentabilidad(resumen.rentabilidadPorcentaje()),
                "chip-positivo", "chip-negativo", "chip-gris");
        avisoDivisas.setText(resumen.tiposCambioDesactualizados()
                ? "⚠ Algún tipo de cambio tiene más de 7 días: el patrimonio es aproximado"
                : "Efectivo disponible: " + Formato.moneda(resumen.saldoDisponible(), simbolo));

        tablaPosiciones.getItems().setAll(resumen.posiciones());
        tablaHistorial.getItems().setAll(simulador.historial(Sesion.getUsuario().getIdUsuario()));
        botonVender.setDisable(resumen.posiciones().isEmpty());
    }

    private void configurarFiltros() {
        List<Opcion<TipoActivo>> tipos = new ArrayList<>();
        tipos.add(new Opcion<>("Todos los tipos", null));
        for (TipoActivo t : TipoActivo.values()) {
            tipos.add(new Opcion<>(nombreTipo(t), t));
        }
        filtroTipo.getItems().setAll(tipos);
        filtroTipo.getSelectionModel().selectFirst();

        List<Opcion<Divisa>> opcionesDivisa = new ArrayList<>();
        opcionesDivisa.add(new Opcion<>("Todas las divisas", null));
        divisas.values().forEach(d -> opcionesDivisa.add(new Opcion<>(d.getCodigo(), d)));
        filtroDivisa.getItems().setAll(opcionesDivisa);
        filtroDivisa.getSelectionModel().selectFirst();

        campoBusqueda.textProperty().addListener((obs, a, b) -> aplicarFiltros());
        filtroTipo.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        filtroDivisa.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        aplicarFiltros();
    }

    /** CU-03: el filtrado se hace en memoria sobre el catálogo ya cargado. */
    private void aplicarFiltros() {
        tablaCatalogo.getItems().setAll(simulador.filtrar(catalogo, campoBusqueda.getText(),
                valor(filtroTipo), valor(filtroDivisa)));
    }

    // ------------------------------------------------------------------ orden

    @FXML
    private void modoCompra() {
        toggleCompra.setSelected(true);
        campoCantidad.requestFocus();
    }

    @FXML
    private void modoVenta() {
        toggleVenta.setSelected(true);
        PosicionDetalle posicion = tablaPosiciones.getSelectionModel().getSelectedItem();
        if (posicion == null && !resumen.posiciones().isEmpty() && titulosPoseidos(activoSeleccionado).signum() == 0) {
            tablaPosiciones.getSelectionModel().selectFirst();
        }
        campoCantidad.requestFocus();
    }

    private void actualizarModo() {
        boolean venta = esVenta();
        botonConfirmar.setText(venta ? "Confirmar venta" : "Confirmar compra");
        estilo(botonConfirmar, venta ? "boton-venta" : "boton-primario", "boton-venta", "boton-primario");
        botonMaximo.setVisible(venta);
        botonMaximo.setManaged(venta);
        recalcular();
    }

    private void seleccionar(Activo activo) {
        activoSeleccionado = activo;
        mostrarActivo();
        recalcular();
    }

    private void mostrarActivo() {
        boolean hay = activoSeleccionado != null;
        ordenNombre.setVisible(hay);
        ordenPrecio.setVisible(hay);
        ordenPoseidos.setVisible(hay);
        if (!hay) {
            ordenTicker.setText("Ningún activo seleccionado");
            ordenNombre.setText("");
            ordenPrecio.setText("");
            ordenPoseidos.setText("Elige un activo en el catálogo o en tu cartera.");
            ordenPoseidos.setVisible(true);
            return;
        }
        Divisa divisa = divisas.get(activoSeleccionado.getIdDivisa());
        ordenTicker.setText(activoSeleccionado.getTicker() + "  ·  " + nombreTipo(activoSeleccionado.getTipoActivo()));
        ordenNombre.setText(activoSeleccionado.getNombre());
        ordenPrecio.setText(Formato.moneda(activoSeleccionado.getPrecioActual(), divisa.getSimbolo()) + " por título");
        BigDecimal poseidos = titulosPoseidos(activoSeleccionado);
        ordenPoseidos.setText("En tu cartera: " + formatoCantidad(poseidos) + " títulos");
    }

    /** Recalcula el importe de la orden en cada cambio (FA-01) y habilita o no el botón (6.9.2, 6.9.3). */
    private void recalcular() {
        ocultarMensaje();
        campoCantidad.getStyleClass().remove(ESTILO_ERROR);
        desglose.getChildren().clear();
        desglose.setVisible(false);
        desglose.setManaged(false);
        botonConfirmar.setDisable(true);
        if (activoSeleccionado == null || resumen == null || campoCantidad.getText().isBlank()) {
            return;
        }

        BigDecimal cantidad;
        try {
            cantidad = simulador.validarCantidad(campoCantidad.getText());
        } catch (ValidacionException e) {
            campoCantidad.getStyleClass().add(ESTILO_ERROR);
            mostrarMensaje(e.getMessage(), false);
            return;
        }

        Divisa divisaActivo = divisas.get(activoSeleccionado.getIdDivisa());
        Divisa base = resumen.divisaBase();
        Cotizacion c = simulador.cotizar(esVenta() ? TipoOperacion.VENTA : TipoOperacion.COMPRA, activoSeleccionado,
                divisaActivo, base, cantidad, resumen.saldoDisponible(), titulosPoseidos(activoSeleccionado));

        int fila = 0;
        if (divisaActivo.getIdDivisa() != base.getIdDivisa()) {
            filaDesglose(fila++, "Importe en " + divisaActivo.getCodigo(),
                    Formato.moneda(c.importeActivo(), divisaActivo.getSimbolo()), false);
        }
        filaDesglose(fila++, esVenta() ? "Recibirás" : "Coste total",
                Formato.moneda(c.importeBase(), base.getSimbolo()), true);
        filaDesglose(fila++, "Saldo disponible", Formato.moneda(resumen.saldoDisponible(), base.getSimbolo()), false);
        BigDecimal saldoFinal = esVenta()
                ? resumen.saldoDisponible().add(c.importeBase())
                : resumen.saldoDisponible().subtract(c.importeBase());
        filaDesglose(fila, "Saldo tras la orden", Formato.moneda(saldoFinal, base.getSimbolo()), false);
        desglose.setVisible(true);
        desglose.setManaged(true);

        if (!c.permitida()) {
            campoCantidad.getStyleClass().add(ESTILO_ERROR);
            mostrarMensaje(c.motivo(), false);
            return;
        }
        botonConfirmar.setDisable(false);
    }

    @FXML
    private void confirmar() {
        if (activoSeleccionado == null) {
            return;
        }
        try {
            BigDecimal cantidad = simulador.validarCantidad(campoCantidad.getText());
            int idUsuario = Sesion.getUsuario().getIdUsuario();
            ResultadoOperacion resultado = esVenta()
                    ? simulador.vender(idUsuario, activoSeleccionado.getIdActivo(), cantidad)
                    : simulador.comprar(idUsuario, activoSeleccionado.getIdActivo(), cantidad);

            Sesion.getUsuario().setSaldoFicticio(resultado.saldoRestante());
            String simbolo = resumen.divisaBase().getSimbolo();
            recargarCartera();
            mostrarActivo();
            campoCantidad.clear(); // dispara recalcular(), que oculta mensajes: el de éxito va después
            mostrarMensaje((esVenta() ? "Venta realizada: " : "Compra realizada: ")
                    + formatoCantidad(cantidad) + " " + activoSeleccionado.getTicker() + " por "
                    + Formato.moneda(resultado.importeBase(), simbolo) + ".", true);
        } catch (ValidacionException e) {
            campoCantidad.getStyleClass().add(ESTILO_ERROR);
            mostrarMensaje(e.getMessage(), false);
        } catch (DataAccessException e) {
            // E-02: la transacción se ha deshecho; no ha cambiado nada
            mostrarMensaje(e.getMessage(), false);
        }
    }

    /** FA-02: se cancela la orden sin modificar saldo ni cartera. */
    @FXML
    private void cancelar() {
        tablaCatalogo.getSelectionModel().clearSelection();
        tablaPosiciones.getSelectionModel().clearSelection();
        activoSeleccionado = null;
        campoCantidad.clear();
        mostrarActivo();
        recalcular();
    }

    /** Rellena la cantidad con todos los títulos poseídos (venta total). */
    @FXML
    private void venderTodo() {
        BigDecimal poseidos = titulosPoseidos(activoSeleccionado);
        if (poseidos.signum() > 0) {
            campoCantidad.setText(formatoCantidad(poseidos));
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaPosiciones() {
        tablaPosiciones.getColumns().setAll(List.of(
                columnaDosLineas("Activo", p -> p.activo().getTicker(), p -> p.activo().getNombre()),
                columna("Títulos", p -> formatoCantidad(p.cantidad())),
                columna("Precio medio", p -> Formato.moneda(p.precioMedio(), p.divisa().getSimbolo())),
                columna("Precio actual", p -> Formato.moneda(p.activo().getPrecioActual(), p.divisa().getSimbolo())),
                columna("Valor", p -> Formato.moneda(p.valorActual(), p.divisa().getSimbolo())),
                columnaPnL()));
        tablaPosiciones.setPlaceholder(new Label("Aún no tienes posiciones. Elige un activo del catálogo para comprar."));
        tablaPosiciones.getSelectionModel().selectedItemProperty().addListener((obs, a, p) -> {
            if (p != null) {
                tablaCatalogo.getSelectionModel().clearSelection();
                seleccionar(p.activo());
            }
        });
        tablaPosiciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaPosiciones.setFixedCellSize(ALTO_FILA);
    }

    private void configurarTablaCatalogo() {
        tablaCatalogo.getColumns().setAll(List.of(
                columna("Ticker", Activo::getTicker),
                columna("Nombre", Activo::getNombre),
                columna("Tipo", a -> nombreTipo(a.getTipoActivo())),
                columna("Divisa", a -> divisas.get(a.getIdDivisa()).getCodigo()),
                columna("Precio", a -> Formato.moneda(a.getPrecioActual(), divisas.get(a.getIdDivisa()).getSimbolo()))));
        tablaCatalogo.setPlaceholder(new Label("Ningún activo coincide con la búsqueda."));
        tablaCatalogo.getSelectionModel().selectedItemProperty().addListener((obs, a, activo) -> {
            if (activo != null) {
                tablaPosiciones.getSelectionModel().clearSelection();
                seleccionar(activo);
            }
        });
        tablaCatalogo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaCatalogo.setFixedCellSize(ALTO_FILA);
    }

    private void configurarTablaHistorial() {
        TableColumn<OperacionDetalle, String> tipo = columna("Tipo", o -> o.tipo() == TipoOperacion.COMPRA ? "Compra" : "Venta");
        tipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                setText(vacia ? null : texto);
                getStyleClass().removeAll("texto-positivo", "texto-negativo");
                if (!vacia) {
                    getStyleClass().add("Compra".equals(texto) ? "texto-positivo" : "texto-negativo");
                }
            }
        });
        TableColumn<OperacionDetalle, String> pnl = columna("P&L realizado", o -> o.pnlRealizado() == null
                ? "—" : (o.pnlRealizado().signum() > 0 ? "+" : "") + Formato.moneda(o.pnlRealizado(), o.simbolo()));
        tablaHistorial.getColumns().setAll(List.of(
                columna("Fecha", o -> o.fecha() == null ? "" : o.fecha().format(FORMATO_FECHA)),
                tipo,
                columnaDosLineas("Activo", OperacionDetalle::ticker, OperacionDetalle::nombre),
                columna("Títulos", o -> formatoCantidad(o.cantidad())),
                columna("Precio", o -> Formato.moneda(o.precio(), o.simbolo())),
                columna("Importe", o -> Formato.moneda(o.importe(), o.simbolo())),
                pnl));
        tablaHistorial.setPlaceholder(new Label("Todavía no has realizado ninguna operación."));
        tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaHistorial.setFixedCellSize(ALTO_FILA);
    }

    private TableColumn<PosicionDetalle, String> columnaPnL() {
        TableColumn<PosicionDetalle, String> col = columna("Gan. / Pérd.",
                p -> (p.pnl().signum() > 0 ? "+" : "") + Formato.moneda(p.pnl(), p.divisa().getSimbolo()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                if (vacia || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                PosicionDetalle p = getTableRow().getItem();
                String estilo = p.pnl().signum() >= 0 ? "texto-positivo" : "texto-negativo";
                Label importe = new Label(texto);
                Label pct = new Label(Formato.porcentaje(p.pnlPorcentaje(), true));
                pct.setWrapText(false);
                importe.getStyleClass().add(estilo);
                pct.getStyleClass().addAll("nota", estilo);
                setText(null);
                setGraphic(new VBox(importe, pct));
            }
        });
        return col;
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor) {
        TableColumn<T, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        col.setReorderable(false);
        return col;
    }

    /** Columna con texto principal en negrita y una segunda línea secundaria (ticker + nombre). */
    private static <T> TableColumn<T, String> columnaDosLineas(String titulo, Function<T, String> principal,
                                                              Function<T, String> secundaria) {
        TableColumn<T, String> col = columna(titulo, principal);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                if (vacia || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label linea1 = new Label(texto);
                linea1.getStyleClass().add("tabla-ticker");
                Label linea2 = new Label(secundaria.apply(getTableRow().getItem()));
                linea2.getStyleClass().add("nota");
                linea2.setWrapText(false); // .nota ajusta líneas y, dentro de una celda, inflaría la fila
                setText(null);
                setGraphic(new VBox(linea1, linea2));
            }
        });
        return col;
    }

    // ------------------------------------------------------------------ auxiliares

    private void filaDesglose(int fila, String concepto, String importe, boolean destacado) {
        Label etiqueta = new Label(concepto);
        etiqueta.getStyleClass().add(destacado ? "desglose-total" : "nota");
        Label valor = new Label(importe);
        valor.getStyleClass().add(destacado ? "desglose-total" : "desglose-valor");
        GridPane.setHgrow(etiqueta, Priority.ALWAYS);
        desglose.add(etiqueta, 0, fila);
        desglose.add(valor, 1, fila);
    }

    private BigDecimal titulosPoseidos(Activo activo) {
        if (activo == null || resumen == null) {
            return BigDecimal.ZERO;
        }
        return resumen.posiciones().stream()
                .filter(p -> p.activo().getIdActivo() == activo.getIdActivo())
                .map(PosicionDetalle::cantidad)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private boolean esVenta() {
        return toggleVenta.isSelected();
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensajeOrden.setText(texto);
        estilo(mensajeOrden, exito ? "mensaje-exito" : "mensaje-error", "mensaje-exito", "mensaje-error");
        mensajeOrden.setVisible(true);
        mensajeOrden.setManaged(true);
    }

    private void ocultarMensaje() {
        mensajeOrden.setVisible(false);
        mensajeOrden.setManaged(false);
    }

    private static <T> T valor(ComboBox<Opcion<T>> combo) {
        Opcion<T> opcion = combo.getValue();
        return opcion == null ? null : opcion.valor();
    }

    private static String formatoCantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Verde si gana, rojo si pierde y gris si el porcentaje redondeado es 0 (p. ej. −1 céntimo por redondeos). */
    static String estiloRentabilidad(BigDecimal porcentaje) {
        return switch (porcentaje.signum()) {
            case 1 -> "chip-positivo";
            case -1 -> "chip-negativo";
            default -> "chip-gris";
        };
    }

    static String nombreTipo(TipoActivo tipo) {
        return switch (tipo) {
            case ACCION -> "Acción";
            case ETF -> "ETF";
            case FONDO -> "Fondo";
            case CRIPTO -> "Cripto";
        };
    }

    private static void estilo(javafx.scene.Node nodo, String nueva, String... posibles) {
        nodo.getStyleClass().removeAll(posibles);
        nodo.getStyleClass().add(nueva);
    }
}
