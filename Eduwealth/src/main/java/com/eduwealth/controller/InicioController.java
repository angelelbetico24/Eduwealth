package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Broker;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.AutenticacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ResumenPanel;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Panel Principal: resumen de patrimonio, presupuesto del mes, medidor de preparación,
 * cartera, novedades y broker recomendado. Solo muestra datos; los cálculos están en PanelService.
 */
public class InicioController {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES);
    private static final String[] CABECERAS = {"Activo", "Cantidad", "Precio medio", "Precio actual", "Gan. / Pérd."};

    @FXML private Label saludo;
    @FXML private Label fecha;
    @FXML private Label saldoCabecera;
    @FXML private Label avisoError;
    @FXML private Label patrimonio;
    @FXML private Label rentabilidadPct;
    @FXML private Label rentabilidad;
    @FXML private Label capitalInicial;
    @FXML private Label gastoMes;
    @FXML private Label limiteMes;
    @FXML private ProgressBar barraPresupuesto;
    @FXML private Label estadoPresupuesto;
    @FXML private Label restantePresupuesto;
    @FXML private Label preparacion;
    @FXML private Label estadoPreparacion;
    @FXML private ProgressBar barraPreparacion;
    @FXML private Label leccionesSuperadas;
    @FXML private Label subtituloCartera;
    @FXML private GridPane tablaCartera;
    @FXML private Label valorCartera;
    @FXML private Label efectivo;
    @FXML private VBox novedades;
    @FXML private HBox tarjetaBroker;
    @FXML private Label brokerIniciales;
    @FXML private Label brokerMotivo;
    @FXML private Label brokerNombre;
    @FXML private Label brokerNivel;
    @FXML private Label brokerDescripcion;
    @FXML private Label brokerComisiones;
    @FXML private Label brokerRegulacion;

    private Consumer<Seccion> navegacion = s -> { };
    private String simbolo = "";

    @FXML
    private void initialize() {
        Usuario usuario = Sesion.getUsuario();
        LocalDate hoy = LocalDate.now();
        saludo.setText("¡Hola, " + primerNombre(usuario.getNombre()) + "!");
        fecha.setText(capitalizar(hoy.format(FORMATO_FECHA)));
        try {
            ResumenPanel resumen = FabricaServicios.panel().construirResumen(usuario, hoy);
            simbolo = resumen.divisaBase().getSimbolo();
            mostrarCabecera(resumen);
            mostrarPatrimonio(resumen);
            mostrarPresupuesto(resumen.presupuestos());
            mostrarPreparacion(resumen.educacion());
            mostrarCartera(resumen);
            mostrarNovedades(resumen);
            mostrarBroker(resumen.brokerRecomendado());
        } catch (DataAccessException e) {
            avisoError.setText(e.getMessage());
            avisoError.setVisible(true);
            avisoError.setManaged(true);
        }
    }

    /** El MainController indica cómo cambiar de sección desde los botones del panel. */
    public void setNavegacion(Consumer<Seccion> navegacion) {
        this.navegacion = navegacion;
    }

    private void mostrarCabecera(ResumenPanel r) {
        saldoCabecera.setText("Saldo virtual:  " + dinero(r.saldoDisponible()));
    }

    private void mostrarPatrimonio(ResumenPanel r) {
        patrimonio.setText(dinero(r.patrimonioTotal()));
        boolean positiva = r.rentabilidad().signum() >= 0;
        rentabilidadPct.setText(Formato.porcentaje(r.rentabilidadPorcentaje(), true));
        cambiarEstilo(rentabilidadPct, positiva ? "chip-positivo" : "chip-negativo", "chip-positivo", "chip-negativo");
        rentabilidad.setText((positiva ? "+" : "") + dinero(r.rentabilidad()) + " de rentabilidad");
        cambiarEstilo(rentabilidad, positiva ? "texto-positivo" : "texto-negativo", "texto-positivo", "texto-negativo");
        capitalInicial.setText("Sobre " + dinero(AutenticacionService.SALDO_INICIAL) + " iniciales");
    }

    private void mostrarPresupuesto(ResumenPanel.Presupuestos p) {
        if (p.estado() == null) {
            gastoMes.setText("—");
            limiteMes.setText("");
            barraPresupuesto.setProgress(0);
            estadoPresupuesto.setText("Sin presupuestos");
            cambiarEstilo(estadoPresupuesto, "chip-gris", "chip-verde", "chip-ambar", "chip-rojo", "chip-gris");
            restantePresupuesto.setText("Define tus límites en Presupuestos");
            return;
        }
        gastoMes.setText(dinero(p.gastado()));
        limiteMes.setText("/ " + dinero(p.limite()));
        barraPresupuesto.setProgress(Math.min(p.porcentaje().doubleValue() / 100, 1));
        estadoPresupuesto.setText(p.estado().getEtiqueta() + " (" + Formato.porcentaje(p.porcentaje(), false) + ")");
        String estilo = estiloEstado(p.estado());
        cambiarEstilo(estadoPresupuesto, "chip-" + estilo, "chip-verde", "chip-ambar", "chip-rojo", "chip-gris");
        cambiarEstilo(barraPresupuesto, "barra-" + estilo, "barra-verde", "barra-ambar", "barra-rojo");
        BigDecimal restante = p.limite().subtract(p.gastado());
        restantePresupuesto.setText(restante.signum() >= 0
                ? "Quedan " + dinero(restante)
                : "Excedido en " + dinero(restante.negate()));
    }

    private void mostrarPreparacion(ResumenPanel.Educacion e) {
        preparacion.setText(e.preparacion() + " %");
        estadoPreparacion.setText(e.listo() ? "Listo" : "En formación");
        cambiarEstilo(estadoPreparacion, e.listo() ? "chip-verde" : "chip-gris", "chip-verde", "chip-gris");
        barraPreparacion.setProgress(e.preparacion() / 100.0);
        leccionesSuperadas.setText("✓ " + e.aprobadas() + " de " + e.totales() + " lecciones superadas");
    }

    private void mostrarCartera(ResumenPanel r) {
        List<ResumenPanel.FilaCartera> filas = r.cartera();
        subtituloCartera.setText(filas.size() + (filas.size() == 1 ? " activo" : " activos")
                + " · importes en la divisa de cada activo");
        valorCartera.setText("Valor de cartera:  " + dinero(r.valorCartera()));
        efectivo.setText("Efectivo disponible:  " + dinero(r.saldoDisponible()));

        tablaCartera.getChildren().clear();
        tablaCartera.getColumnConstraints().setAll(
                columna(30, HPos.LEFT), columna(13, HPos.RIGHT), columna(19, HPos.RIGHT),
                columna(19, HPos.RIGHT), columna(19, HPos.RIGHT));
        for (int c = 0; c < CABECERAS.length; c++) {
            tablaCartera.add(celda(new Label(CABECERAS[c]), "tabla-cabecera"), c, 0);
        }
        if (filas.isEmpty()) {
            Label vacio = new Label("Aún no tienes posiciones. Empieza a practicar en el Simulador.");
            vacio.getStyleClass().add("nota");
            tablaCartera.add(celda(vacio, "tabla-celda"), 0, 1, CABECERAS.length, 1);
            return;
        }
        int fila = 1;
        for (ResumenPanel.FilaCartera f : filas) {
            Label ticker = new Label(f.ticker());
            ticker.getStyleClass().add("tabla-ticker");
            Label nombre = new Label(f.nombre());
            nombre.getStyleClass().add("nota");
            tablaCartera.add(celda(new VBox(ticker, nombre), "tabla-celda"), 0, fila);
            tablaCartera.add(celda(new Label(f.cantidad().stripTrailingZeros().toPlainString()), "tabla-celda"), 1, fila);
            tablaCartera.add(celda(new Label(Formato.moneda(f.precioMedio(), f.simbolo())), "tabla-celda"), 2, fila);
            tablaCartera.add(celda(new Label(Formato.moneda(f.precioActual(), f.simbolo())), "tabla-celda"), 3, fila);

            boolean positivo = f.pnl().signum() >= 0;
            Label pnl = new Label((positivo ? "+" : "") + Formato.moneda(f.pnl(), f.simbolo()));
            Label pct = new Label(Formato.porcentaje(f.pnlPorcentaje(), true));
            pnl.getStyleClass().add(positivo ? "texto-positivo" : "texto-negativo");
            pct.getStyleClass().addAll("nota", positivo ? "texto-positivo" : "texto-negativo");
            VBox celdaPnl = new VBox(pnl, pct);
            celdaPnl.setAlignment(Pos.CENTER_RIGHT);
            tablaCartera.add(celda(celdaPnl, "tabla-celda"), 4, fila);
            fila++;
        }
    }

    private void mostrarNovedades(ResumenPanel r) {
        novedades.getChildren().clear();
        for (ResumenPanel.Aviso aviso : r.presupuestos().avisos()) {
            String estilo = estiloEstado(aviso.estado());
            novedades.getChildren().add(novedad(estilo, aviso.estado() == EstadoPresupuesto.EXCEDIDO ? "✖" : "⚠",
                    (aviso.estado() == EstadoPresupuesto.EXCEDIDO ? "Presupuesto excedido: " : "Alerta de presupuesto: ")
                            + aviso.categoria() + " al " + Formato.porcentaje(aviso.porcentaje(), false),
                    dinero(aviso.gastado()) + " de " + dinero(aviso.limite()),
                    Seccion.PRESUPUESTOS));
        }
        if (r.educacion().siguienteLeccion() != null) {
            novedades.getChildren().add(novedad("azul", "✎",
                    "Continuar lección: " + r.educacion().siguienteLeccion().getTitulo(),
                    "Nivel " + r.educacion().siguienteLeccion().getNivel().name().toLowerCase(ES)
                            + " · sigue creciendo",
                    Seccion.EDUCACION));
        }
        if (r.tiposCambioDesactualizados()) {
            novedades.getChildren().add(novedad("ambar", "⚠", "Tipos de cambio desactualizados",
                    "Algún tipo de cambio tiene más de 7 días: el patrimonio es aproximado.", null));
        }
        if (novedades.getChildren().isEmpty()) {
            Label nada = new Label("Todo en orden. ¡Sigue así!");
            nada.getStyleClass().add("nota");
            novedades.getChildren().add(nada);
        }
    }

    private void mostrarBroker(Broker broker) {
        if (broker == null) {
            tarjetaBroker.setVisible(false);
            tarjetaBroker.setManaged(false);
            return;
        }
        brokerIniciales.setText(MainController.calcularIniciales(broker.getNombre()));
        brokerMotivo.setText("Broker recomendado para tu nivel");
        brokerNombre.setText(broker.getNombre());
        brokerNivel.setText(capitalizar(broker.getNivelRecomendado().name().toLowerCase(ES)));
        brokerDescripcion.setText(broker.getDescripcion());
        brokerComisiones.setText("✓ " + broker.getComisionesInfo());
        brokerRegulacion.setText("✓ Regulado: " + broker.getRegulacion());
    }

    private HBox novedad(String estilo, String icono, String titulo, String detalle, Seccion destino) {
        Label iconoLabel = new Label(icono);
        iconoLabel.getStyleClass().add("novedad-icono");
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("novedad-titulo");
        tituloLabel.setWrapText(true);
        Label detalleLabel = new Label(detalle);
        detalleLabel.getStyleClass().add("nota");
        detalleLabel.setWrapText(true);
        VBox textos = new VBox(2, tituloLabel, detalleLabel);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox caja = new HBox(10, iconoLabel, textos);
        caja.getStyleClass().addAll("novedad", "novedad-" + estilo);
        if (destino != null) {
            Label flecha = new Label("›");
            flecha.getStyleClass().add("novedad-flecha");
            caja.getChildren().add(flecha);
            caja.setOnMouseClicked(e -> navegacion.accept(destino));
            caja.getStyleClass().add("clicable");
        }
        return caja;
    }

    @FXML
    private void irSimulador() {
        navegacion.accept(Seccion.SIMULADOR);
    }

    @FXML
    private void irPresupuestos() {
        navegacion.accept(Seccion.PRESUPUESTOS);
    }

    @FXML
    private void irBrokers() {
        navegacion.accept(Seccion.BROKERS);
    }

    private String dinero(BigDecimal importe) {
        return Formato.moneda(importe, simbolo);
    }

    private static String estiloEstado(EstadoPresupuesto estado) {
        return switch (estado) {
            case DENTRO_LIMITE -> "verde";
            case EN_ALERTA -> "ambar";
            case EXCEDIDO -> "rojo";
        };
    }

    private static Node celda(Node contenido, String estilo) {
        HBox celda = new HBox(contenido);
        celda.getStyleClass().add(estilo);
        celda.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(celda, Priority.ALWAYS);
        return celda;
    }

    private static ColumnConstraints columna(double porcentaje, HPos alineacion) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(porcentaje);
        c.setHalignment(alineacion);
        return c;
    }

    /** Sustituye en el nodo las clases de estilo indicadas por {@code nueva}. */
    private static void cambiarEstilo(Node nodo, String nueva, String... posibles) {
        nodo.getStyleClass().removeAll(posibles);
        nodo.getStyleClass().add(nueva);
    }

    private static String primerNombre(String nombre) {
        return nombre.trim().split("\\s+")[0];
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
