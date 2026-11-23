package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Gasto;
import com.eduwealth.service.CategoriaPresupuesto;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.PresupuestoService;
import com.eduwealth.service.ResultadoGasto;
import com.eduwealth.service.ResumenPresupuestos;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pantalla de presupuestos y gastos. Muestra el mes seleccionado y permite anotar gastos (CU-11).
 * Las reglas (RN-03, validaciones, C-09) están en {@link PresupuestoService}.
 */
public class PresupuestosController {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM", ES);
    private static final DateTimeFormatter FECHA_CAMPO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ESTILO_ERROR = "campo-error";
    private static final int MESES_ATRAS = 12;
    private static final int MESES_ADELANTE = 3;

    private final PresupuestoService servicio = FabricaServicios.presupuestos();

    @FXML private ComboBox<YearMonth> selectorMes;
    @FXML private Label avisoError;
    @FXML private Label totalLimite;
    @FXML private Label numeroCategorias;
    @FXML private Label totalGastado;
    @FXML private ProgressBar barraTotal;
    @FXML private Label porcentajeTotal;
    @FXML private Label totalDisponible;
    @FXML private Label disponibleNota;
    @FXML private Label subtituloCategorias;
    @FXML private VBox listaCategorias;
    @FXML private VBox tarjetaGasto;
    @FXML private TextField campoImporte;
    @FXML private DatePicker campoFecha;
    @FXML private ComboBox<CategoriaGasto> campoCategoria;
    @FXML private TextField campoConcepto;
    @FXML private Label mensajeGasto;
    @FXML private Button botonGuardar;
    @FXML private VBox listaGastos;

    private Map<Integer, CategoriaGasto> categorias = Map.of();
    private String simbolo = "€";
    private Map<String, Node> controlesPorCampo;

    @FXML
    private void initialize() {
        controlesPorCampo = Map.of(
                "importe", campoImporte, "fecha", campoFecha, "categoria", campoCategoria, "concepto", campoConcepto);
        configurarSelectorMes();
        configurarFecha();
        try {
            categorias = servicio.categorias().stream()
                    .collect(Collectors.toMap(CategoriaGasto::getIdCategoria, Function.identity()));
            simbolo = FabricaServicios.divisas().buscarPorId(Sesion.getUsuario().getIdDivisaBase())
                    .map(d -> d.getSimbolo()).orElse("€");
            selectorMes.setValue(YearMonth.now()); // dispara cargarMes()
        } catch (DataAccessException e) {
            avisoError.setText(e.getMessage());
            avisoError.setVisible(true);
            avisoError.setManaged(true);
        }
    }

    // ------------------------------------------------------------------ mes seleccionado

    private void configurarSelectorMes() {
        YearMonth actual = YearMonth.now();
        List<YearMonth> meses = new ArrayList<>();
        for (int i = -MESES_ATRAS; i <= MESES_ADELANTE; i++) {
            meses.add(actual.plusMonths(i));
        }
        selectorMes.getItems().setAll(meses);
        selectorMes.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth mes) {
                return mes == null ? "" : capitalizar(PresupuestoService.nombreMes(mes));
            }

            @Override
            public YearMonth fromString(String texto) {
                return null;
            }
        });
        selectorMes.valueProperty().addListener((obs, a, mes) -> {
            if (mes != null) {
                cargarMes(mes);
            }
        });
    }

    @FXML
    private void mesAnterior() {
        moverMes(-1);
    }

    @FXML
    private void mesSiguiente() {
        moverMes(1);
    }

    private void moverMes(int delta) {
        int i = selectorMes.getItems().indexOf(selectorMes.getValue()) + delta;
        if (i >= 0 && i < selectorMes.getItems().size()) {
            selectorMes.setValue(selectorMes.getItems().get(i));
        }
    }

    private YearMonth mes() {
        return selectorMes.getValue();
    }

    private void cargarMes(YearMonth mes) {
        try {
            ResumenPresupuestos resumen = servicio.resumenDelMes(Sesion.getUsuario().getIdUsuario(), mes);
            mostrarTotales(resumen);
            mostrarCategorias(resumen);
            prepararFormulario(resumen);
            mostrarGastos(servicio.gastosDelMes(Sesion.getUsuario().getIdUsuario(), mes));
        } catch (DataAccessException e) {
            mostrarMensaje(e.getMessage(), "mensaje-error");
        }
    }

    private void mostrarTotales(ResumenPresupuestos r) {
        totalLimite.setText(dinero(r.limite()));
        int n = r.categorias().size();
        numeroCategorias.setText(n == 0 ? "Sin presupuestos este mes"
                : "Distribuido en " + n + (n == 1 ? " categoría" : " categorías"));
        totalGastado.setText(dinero(r.gastado()));
        barraTotal.setProgress(Math.min(r.porcentaje().doubleValue() / 100, 1));
        porcentajeTotal.setText(Formato.porcentaje(r.porcentaje(), false));
        estilo(barraTotal, "barra-" + estiloEstado(r.estado()), "barra-verde", "barra-ambar", "barra-rojo", "barra-azul");

        totalDisponible.setText(dinero(r.disponible()));
        boolean excedido = r.disponible().signum() < 0;
        estilo(totalDisponible, excedido ? "texto-negativo" : "texto-positivo", "texto-positivo", "texto-negativo");
        if (r.vacio()) {
            disponibleNota.setText("Define tus límites para empezar a controlar tus gastos");
        } else if (excedido) {
            disponibleNota.setText("Has superado tu presupuesto mensual");
        } else {
            BigDecimal pct = BigDecimal.valueOf(100).subtract(r.porcentaje()).max(BigDecimal.ZERO);
            disponibleNota.setText(Formato.porcentaje(pct, false) + " de tu presupuesto mensual");
        }
    }

    private void mostrarCategorias(ResumenPresupuestos r) {
        listaCategorias.getChildren().clear();
        subtituloCategorias.setText("Así se distribuyen tus gastos en " + PresupuestoService.nombreMes(r.mes()) + ".");
        if (r.vacio()) {
            Label vacio = new Label("No has definido presupuestos para " + PresupuestoService.nombreMes(r.mes())
                    + ". Fija un límite por categoría para recibir alertas al 80 % y al 100 %.");
            vacio.getStyleClass().add("nota");
            vacio.setWrapText(true);
            listaCategorias.getChildren().add(vacio);
            return;
        }
        for (CategoriaPresupuesto c : r.categorias()) {
            listaCategorias.getChildren().add(filaCategoria(c));
        }
    }

    private VBox filaCategoria(CategoriaPresupuesto c) {
        Label nombre = new Label(c.categoria());
        nombre.getStyleClass().add("tarjeta-subtitulo");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label chip = new Label(etiquetaEstado(c));
        chip.getStyleClass().add("chip-" + estiloEstado(c.estado()));
        HBox cabecera = new HBox(10, nombre, hueco, chip);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label importes = new Label(dinero(c.gastado()));
        importes.getStyleClass().add("categoria-importe");
        Label limite = new Label("/ " + dinero(c.limite()));
        limite.getStyleClass().add("nota");
        Region hueco2 = new Region();
        HBox.setHgrow(hueco2, Priority.ALWAYS);
        Label pct = new Label(Formato.porcentaje(c.porcentaje(), false));
        pct.getStyleClass().add("categoria-porcentaje-" + estiloEstado(c.estado()));
        HBox cifras = new HBox(6, importes, limite, hueco2, pct);
        cifras.setAlignment(Pos.BASELINE_LEFT);

        ProgressBar barra = new ProgressBar(Math.min(c.porcentaje().doubleValue() / 100, 1));
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.getStyleClass().addAll("barra", "barra-" + estiloEstado(c.estado()));

        Label restante = new Label(textoRestante(c));
        restante.getStyleClass().add("nota");

        VBox fila = new VBox(6, cabecera, cifras, barra, restante);
        fila.getStyleClass().addAll("fila-categoria", "clicable");
        // Al pulsar una categoría se preselecciona en el formulario
        fila.setOnMouseClicked(e -> categorias.values().stream()
                .filter(cat -> cat.getIdCategoria() == c.idCategoria())
                .findFirst()
                .ifPresent(cat -> {
                    campoCategoria.setValue(cat);
                    campoImporte.requestFocus();
                }));
        return fila;
    }

    // ------------------------------------------------------------------ formulario (CU-11)

    private void configurarFecha() {
        campoFecha.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : fecha.format(FECHA_CAMPO);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.trim(), FECHA_CAMPO);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        campoFecha.setPromptText("dd/mm/aaaa");
        // Solo días del mes seleccionado y nunca futuros
        campoFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || mes() == null || !YearMonth.from(fecha).equals(mes())
                        || fecha.isAfter(LocalDate.now()));
            }
        });
    }

    private void prepararFormulario(ResumenPresupuestos r) {
        limpiarErrores();
        ocultarMensaje();
        Set<Integer> conPresupuesto = r.categorias().stream()
                .map(CategoriaPresupuesto::idCategoria).collect(Collectors.toSet());
        CategoriaGasto seleccionada = campoCategoria.getValue();
        campoCategoria.getItems().setAll(categorias.values().stream()
                .filter(c -> conPresupuesto.contains(c.getIdCategoria()))
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .toList());
        campoCategoria.setValue(seleccionada != null && conPresupuesto.contains(seleccionada.getIdCategoria())
                ? seleccionada : null);

        LocalDate hoy = LocalDate.now();
        boolean futuro = r.mes().isAfter(YearMonth.from(hoy));
        campoFecha.setValue(futuro ? null : (r.mes().equals(YearMonth.from(hoy)) ? hoy : r.mes().atEndOfMonth()));

        String motivo = futuro ? "No se pueden anotar gastos de un mes futuro."
                : r.vacio() ? "Para anotar gastos en " + PresupuestoService.nombreMes(r.mes())
                + " necesitas definir antes algún presupuesto." : null;
        boolean habilitado = motivo == null;
        for (Node control : List.of(campoImporte, campoFecha, campoCategoria, campoConcepto, botonGuardar)) {
            control.setDisable(!habilitado);
        }
        if (!habilitado) {
            mostrarMensaje(motivo, "mensaje-aviso");
        }
    }

    @FXML
    private void enfocarFormulario() {
        if (mes() != null && mes().isAfter(YearMonth.now())) {
            selectorMes.setValue(YearMonth.now());
        }
        campoImporte.requestFocus();
    }

    @FXML
    private void guardarGasto() {
        limpiarErrores();
        ocultarMensaje();
        // Si el texto de la fecha no tiene formato válido, el DatePicker no actualiza su valor
        campoFecha.setValue(campoFecha.getConverter().fromString(campoFecha.getEditor().getText()));
        try {
            ResultadoGasto r = servicio.registrarGasto(Sesion.getUsuario().getIdUsuario(), campoCategoria.getValue(),
                    campoImporte.getText(), campoFecha.getValue(), campoConcepto.getText(), LocalDate.now());
            campoImporte.clear();
            campoConcepto.clear();
            cargarMes(mes());
            notificar(r);
        } catch (ValidacionException e) {
            mostrarMensaje(e.getMessage(), "mensaje-error");
            Node control = e.getCampo() == null ? null : controlesPorCampo.get(e.getCampo());
            if (control != null) {
                control.getStyleClass().add(ESTILO_ERROR);
                control.requestFocus();
            }
        } catch (DataAccessException e) {
            mostrarMensaje(e.getMessage(), "mensaje-error");
        }
    }

    /** FA-01: aviso en ámbar al 80 %. FA-02: alerta de prioridad alta al alcanzar el 100 %. */
    private void notificar(ResultadoGasto r) {
        CategoriaPresupuesto c = r.categoria();
        String registrado = "Gasto de " + dinero(r.gasto().getMonto()) + " registrado en " + c.categoria() + ". ";
        switch (c.estado()) {
            case DENTRO_LIMITE -> mostrarMensaje(registrado + "Te quedan " + dinero(c.restante()) + ".", "mensaje-exito");
            case EN_ALERTA -> mostrarMensaje(registrado + "⚠ Alerta: llevas el "
                    + Formato.porcentaje(c.porcentaje(), false) + " del límite; te quedan " + dinero(c.restante()) + ".",
                    "mensaje-aviso");
            case EXCEDIDO -> {
                mostrarMensaje(registrado + "Presupuesto excedido ("
                        + Formato.porcentaje(c.porcentaje(), false) + ").", "mensaje-error");
                if (r.cambioDeEstado()) {
                    Alert alerta = new Alert(Alert.AlertType.WARNING);
                    alerta.setTitle("Presupuesto excedido");
                    alerta.setHeaderText("Has alcanzado el límite de " + c.categoria());
                    alerta.setContentText("Llevas " + dinero(c.gastado()) + " de un presupuesto de "
                            + dinero(c.limite()) + " (" + Formato.porcentaje(c.porcentaje(), false)
                            + "). Revisa tus gastos de esta categoría antes de seguir.");
                    alerta.getButtonTypes().setAll(new ButtonType("Entendido", ButtonBar.ButtonData.OK_DONE));
                    aplicarEstilos(alerta);
                    alerta.showAndWait();
                }
            }
        }
    }

    // ------------------------------------------------------------------ últimos gastos

    private void mostrarGastos(List<Gasto> gastos) {
        listaGastos.getChildren().clear();
        if (gastos.isEmpty()) {
            Label vacio = new Label("Todavía no hay gastos en " + PresupuestoService.nombreMes(mes()) + ".");
            vacio.getStyleClass().add("nota");
            listaGastos.getChildren().add(vacio);
            return;
        }
        for (Gasto g : gastos) {
            listaGastos.getChildren().add(filaGasto(g));
        }
    }

    private HBox filaGasto(Gasto g) {
        Label fecha = new Label(g.getFecha().format(FECHA_CORTA));
        fecha.getStyleClass().add("nota");
        fecha.setMinWidth(48);

        CategoriaGasto categoria = categorias.get(g.getIdCategoria());
        Label chip = new Label(categoria == null ? "?" : categoria.getNombre());
        chip.getStyleClass().add("chip-gris");
        Label concepto = new Label(Objects.requireNonNullElse(g.getConcepto(), ""));
        concepto.getStyleClass().add("nota");
        VBox textos = new VBox(3, chip, concepto);
        HBox.setHgrow(textos, Priority.ALWAYS);
        if (g.getConcepto() == null) {
            textos.getChildren().remove(concepto);
        }

        Label importe = new Label(dinero(g.getMonto()));
        importe.getStyleClass().add("gasto-importe");
        Button borrar = new Button("✕");
        borrar.getStyleClass().add("boton-icono");
        borrar.setOnAction(e -> confirmarBorrado(g));

        HBox fila = new HBox(10, fecha, textos, importe, borrar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("fila-gasto");
        return fila;
    }

    private void confirmarBorrado(Gasto g) {
        ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se eliminará el gasto de " + dinero(g.getMonto()) + " del " + g.getFecha().format(FECHA_CAMPO) + ".",
                eliminar, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        confirmacion.setTitle("Eliminar gasto");
        confirmacion.setHeaderText("¿Eliminar este gasto?");
        aplicarEstilos(confirmacion);
        if (confirmacion.showAndWait().orElse(null) == eliminar) {
            try {
                servicio.eliminarGasto(Sesion.getUsuario().getIdUsuario(), g.getIdGasto());
                cargarMes(mes());
                mostrarMensaje("Gasto eliminado.", "mensaje-exito");
            } catch (ValidacionException | DataAccessException e) {
                mostrarMensaje(e.getMessage(), "mensaje-error");
            }
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private String textoRestante(CategoriaPresupuesto c) {
        int signo = c.restante().signum();
        if (signo > 0) {
            return "Te quedan " + dinero(c.restante());
        }
        return signo == 0 ? "Has alcanzado tu límite" : "Te has pasado " + dinero(c.restante().negate());
    }

    private static String etiquetaEstado(CategoriaPresupuesto c) {
        return switch (c.estado()) {
            case DENTRO_LIMITE -> "Dentro del límite";
            case EN_ALERTA -> "En Alerta (" + Formato.porcentaje(c.porcentaje(), false) + ")";
            case EXCEDIDO -> "Presupuesto Excedido";
        };
    }

    private static String estiloEstado(EstadoPresupuesto estado) {
        if (estado == null) {
            return "azul";
        }
        return switch (estado) {
            case DENTRO_LIMITE -> "verde";
            case EN_ALERTA -> "ambar";
            case EXCEDIDO -> "rojo";
        };
    }

    private void aplicarEstilos(Alert alerta) {
        alerta.getDialogPane().getStylesheets().addAll(tarjetaGasto.getScene().getStylesheets());
        alerta.initOwner(tarjetaGasto.getScene().getWindow());
    }

    private void mostrarMensaje(String texto, String estiloMensaje) {
        mensajeGasto.setText(texto);
        estilo(mensajeGasto, estiloMensaje, "mensaje-error", "mensaje-exito", "mensaje-aviso");
        mensajeGasto.setVisible(true);
        mensajeGasto.setManaged(true);
    }

    private void ocultarMensaje() {
        mensajeGasto.setVisible(false);
        mensajeGasto.setManaged(false);
    }

    private void limpiarErrores() {
        controlesPorCampo.values().forEach(c -> c.getStyleClass().remove(ESTILO_ERROR));
    }

    private String dinero(BigDecimal importe) {
        return Formato.moneda(importe, simbolo);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static void estilo(Node nodo, String nueva, String... posibles) {
        nodo.getStyleClass().removeAll(posibles);
        nodo.getStyleClass().add(nueva);
    }
}
