package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.service.Disponibilidad;
import com.eduwealth.service.EducacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.LeccionRuta;
import com.eduwealth.service.ResumenEducacion;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pantalla del módulo educativo: medidor de preparación (RN-07) y ruta de aprendizaje con las
 * lecciones superadas, disponibles y bloqueadas (RN-08). Los cálculos están en {@link EducacionService}.
 */
public class EducacionController {

    private static final int COLUMNAS = 3;
    private static final double RADIO_ANILLO = 50;
    private static final int LONGITUD_RESUMEN = 130;

    private final EducacionService servicio = FabricaServicios.educacion();

    @FXML private Label saldoCabecera;
    @FXML private Label avisoError;
    @FXML private StackPane anillo;
    @FXML private Label tituloMedidor;
    @FXML private ProgressBar barraMedidor;
    @FXML private Label estadoMedidor;
    @FXML private Label objetivoMedidor;
    @FXML private Label leccionesSuperadas;
    @FXML private Label notaMedia;
    @FXML private ToggleGroup grupoNivel;
    @FXML private ToggleButton filtroTodos;
    @FXML private ToggleButton filtroPrincipiante;
    @FXML private ToggleButton filtroIntermedio;
    @FXML private ToggleButton filtroAvanzado;
    @FXML private Label numeroLecciones;
    @FXML private GridPane rejillaLecciones;
    @FXML private Label mensajeLeccion;

    private List<LeccionRuta> ruta = List.of();
    private Leccion siguiente;
    private Consumer<Leccion> abrirLeccion = l -> mostrarAviso("La vista de la lección estará disponible en breve.");

    @FXML
    private void initialize() {
        for (int i = 0; i < COLUMNAS; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / COLUMNAS);
            rejillaLecciones.getColumnConstraints().add(c);
        }
        // Un filtro siempre seleccionado
        grupoNivel.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                mostrarRuta();
            }
        });
        cargar();
    }

    /** El MainController indica qué hacer al abrir una lección (vista de lección y test, CU-08). */
    public void setAbrirLeccion(Consumer<Leccion> abrirLeccion) {
        this.abrirLeccion = abrirLeccion;
    }

    private void cargar() {
        int idUsuario = Sesion.getUsuario().getIdUsuario();
        try {
            FabricaServicios.divisas().buscarPorId(Sesion.getUsuario().getIdDivisaBase()).ifPresent(d ->
                    saldoCabecera.setText("Saldo virtual:  " + Formato.moneda(Sesion.getUsuario().getSaldoFicticio(),
                            d.getSimbolo())));
            ResumenEducacion resumen = servicio.resumen(idUsuario);
            siguiente = resumen.siguienteLeccion();
            mostrarMedidor(resumen);
            ruta = servicio.rutaDeAprendizaje(idUsuario);
            mostrarRuta();
        } catch (DataAccessException e) {
            avisoError.setText(e.getMessage());
            avisoError.setVisible(true);
            avisoError.setManaged(true);
        }
    }

    // ------------------------------------------------------------------ medidor (RN-07)

    private void mostrarMedidor(ResumenEducacion r) {
        anillo.getChildren().setAll(dibujarAnillo(r.preparacion()));
        tituloMedidor.setText(r.preparacion() + " %  ·  Nivel " + EducacionService.nombreNivel(r.nivelActual()));
        barraMedidor.setProgress(r.preparacion() / 100.0);
        if (r.listo()) {
            estadoMedidor.setText("✓ Listo para dar el salto a la inversión real");
        } else if (r.aprobadas() > 0) {
            estadoMedidor.setText("✓ Apto para operar en cartera simulada");
        } else {
            estadoMedidor.setText("Empieza por la primera lección para subir tu preparación");
        }
        objetivoMedidor.setText(r.siguienteLeccion() != null
                ? "Siguiente objetivo: " + r.siguienteLeccion().getTitulo() + ". La práctica en el simulador "
                + "también suma (hasta un 30 %)."
                : "Has completado todas las lecciones. Sigue practicando en el simulador.");
        leccionesSuperadas.setText(r.aprobadas() + "/" + r.totales());
        notaMedia.setText(r.notaMedia() == null ? "—" : r.notaMedia().toPlainString().replace('.', ',') + "/10");
    }

    /** Anillo de progreso: círculo de fondo y arco proporcional a la preparación, con el porcentaje en el centro. */
    private StackPane dibujarAnillo(int porcentaje) {
        double centro = RADIO_ANILLO + 10;
        Circle fondo = new Circle(centro, centro, RADIO_ANILLO);
        fondo.getStyleClass().add("anillo-fondo");
        Arc arco = new Arc(centro, centro, RADIO_ANILLO, RADIO_ANILLO, 90, -360.0 * porcentaje / 100);
        arco.setType(ArcType.OPEN);
        arco.setStrokeLineCap(StrokeLineCap.ROUND);
        arco.getStyleClass().add("anillo-arco");
        // Pane con tamaño fijo: las figuras se colocan por coordenadas y no se descentran con el arco
        Pane figuras = new Pane(fondo, arco);
        figuras.setPrefSize(centro * 2, centro * 2);
        figuras.setMaxSize(centro * 2, centro * 2);

        Label valor = new Label(porcentaje + "%");
        valor.getStyleClass().add("anillo-valor");
        Label texto = new Label("preparación");
        texto.getStyleClass().add("nota");
        VBox textos = new VBox(0, valor, texto);
        textos.setAlignment(Pos.CENTER);
        return new StackPane(figuras, textos);
    }

    // ------------------------------------------------------------------ ruta de aprendizaje (RN-08)

    private void mostrarRuta() {
        Nivel filtro = nivelFiltrado();
        List<LeccionRuta> visibles = ruta.stream()
                .filter(l -> filtro == null || l.leccion().getNivel() == filtro)
                .toList();
        numeroLecciones.setText(visibles.size() == 1 ? "1 lección" : visibles.size() + " lecciones");

        rejillaLecciones.getChildren().clear();
        for (int i = 0; i < visibles.size(); i++) {
            VBox tarjeta = tarjeta(visibles.get(i));
            GridPane.setFillHeight(tarjeta, true);
            rejillaLecciones.add(tarjeta, i % COLUMNAS, i / COLUMNAS);
        }
    }

    private VBox tarjeta(LeccionRuta l) {
        Leccion leccion = l.leccion();
        boolean esSiguiente = siguiente != null && siguiente.getIdLeccion() == leccion.getIdLeccion();

        Label icono = new Label(iconoNivel(leccion.getNivel()));
        icono.getStyleClass().addAll("leccion-icono", "leccion-icono-" + clase(leccion.getNivel()));
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label nivel = new Label(EducacionService.nombreNivel(leccion.getNivel()));
        nivel.getStyleClass().add("chip-gris");
        HBox cabecera = new HBox(10, icono, hueco, nivel);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label(leccion.getTitulo());
        titulo.getStyleClass().add("leccion-titulo");
        titulo.setWrapText(true);
        Label resumen = new Label(resumen(leccion.getContenido()));
        resumen.getStyleClass().add("nota");
        resumen.setWrapText(true);
        Label datos = new Label("◷  " + l.minutos() + " min  ·  " + l.preguntas() + " preguntas");
        datos.getStyleClass().add("nota");

        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);

        Label estado = new Label(textoEstado(l, esSiguiente));
        estado.getStyleClass().add("estado-" + l.disponibilidad().name().toLowerCase(Locale.ROOT));
        Button accion = botonAccion(l);

        VBox tarjeta = new VBox(10, cabecera, titulo, resumen, datos, empuje, estado, accion);
        tarjeta.getStyleClass().add("tarjeta-leccion");
        if (esSiguiente) {
            tarjeta.getStyleClass().add("tarjeta-leccion-siguiente");
        }
        if (l.disponibilidad() == Disponibilidad.BLOQUEADA) {
            tarjeta.getStyleClass().add("tarjeta-leccion-bloqueada");
        }
        tarjeta.setMaxHeight(Double.MAX_VALUE);
        return tarjeta;
    }

    private Button botonAccion(LeccionRuta l) {
        Button boton = new Button();
        boton.setMaxWidth(Double.MAX_VALUE);
        switch (l.disponibilidad()) {
            case SUPERADA -> {
                boton.setText("Repasar lección");
                boton.getStyleClass().add("boton-secundario");
            }
            case DISPONIBLE -> {
                boton.setText("Realizar lección y test  →");
                boton.getStyleClass().add("boton-primario");
            }
            case BLOQUEADA -> {
                boton.setText("⊘  Bloqueado (completa el nivel previo)");
                boton.getStyleClass().add("boton-bloqueado");
                boton.setDisable(true);
            }
        }
        boton.setOnAction(e -> abrirLeccion.accept(l.leccion()));
        return boton;
    }

    private static String textoEstado(LeccionRuta l, boolean esSiguiente) {
        return switch (l.disponibilidad()) {
            case SUPERADA -> "✓ Superado (" + Formato.porcentaje(l.mejorNota(), false).replace(",0 %", " %") + ")";
            case DISPONIBLE -> l.mejorNota() != null
                    ? "◉ Disponible · mejor nota " + Formato.porcentaje(l.mejorNota(), false).replace(",0 %", " %")
                    : esSiguiente ? "◉ Disponible · Tu siguiente paso" : "◉ Disponible";
            case BLOQUEADA -> "⊘ Pendiente";
        };
    }

    // ------------------------------------------------------------------ auxiliares

    private Nivel nivelFiltrado() {
        Map<ToggleButton, Nivel> niveles = Map.of(
                filtroPrincipiante, Nivel.PRINCIPIANTE,
                filtroIntermedio, Nivel.INTERMEDIO,
                filtroAvanzado, Nivel.AVANZADO);
        return grupoNivel.getSelectedToggle() == filtroTodos ? null : niveles.get(grupoNivel.getSelectedToggle());
    }

    /** Primera frase del contenido (o sus primeros caracteres) como descripción de la tarjeta. */
    private static String resumen(String contenido) {
        if (contenido == null) {
            return "";
        }
        String texto = contenido.strip().split("\\R")[0];
        int punto = texto.indexOf(". ");
        if (punto > 0 && punto < LONGITUD_RESUMEN) {
            return texto.substring(0, punto + 1);
        }
        return texto.length() <= LONGITUD_RESUMEN ? texto : texto.substring(0, LONGITUD_RESUMEN).strip() + "…";
    }

    private static String iconoNivel(Nivel nivel) {
        return switch (nivel) {
            case PRINCIPIANTE -> "▲";
            case INTERMEDIO -> "◆";
            case AVANZADO -> "★";
        };
    }

    private static String clase(Nivel nivel) {
        return nivel.name().toLowerCase(Locale.ROOT);
    }

    private void mostrarAviso(String texto) {
        mensajeLeccion.setText(texto);
        mensajeLeccion.setVisible(true);
        mensajeLeccion.setManaged(true);
    }
}
