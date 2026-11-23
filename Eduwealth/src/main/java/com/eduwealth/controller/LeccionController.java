package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Pregunta;
import com.eduwealth.service.EducacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ResultadoTest;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lección y test (CU-08): lectura del contenido, preguntas tipo test y corrección.
 * Si el usuario sale antes de enviar, no se guarda nada (FA-01). La nota, RN-05, la mejor
 * nota (C-10) y el desbloqueo (RN-08) los resuelve {@link EducacionService#corregirTest}.
 */
public class LeccionController {

    /** Términos en mayúsculas del contenido (ACCIÓN, FONDO DE INVERSIÓN…): se muestran en negrita. */
    private static final Pattern CONCEPTO = Pattern.compile("\\b\\p{Lu}{3,}(?:\\s+\\p{Lu}{2,})*\\b");
    private static final char[] LETRAS = {'A', 'B', 'C', 'D'};

    private final EducacionService servicio = FabricaServicios.educacion();

    @FXML private ScrollPane desplazamiento;
    @FXML private Label chipNivel;
    @FXML private Label datosLeccion;
    @FXML private Label tituloLeccion;
    @FXML private Label chipFase;
    @FXML private Label avisoError;
    @FXML private VBox faseLectura;
    @FXML private VBox contenidoLeccion;
    @FXML private Button botonIniciarTest;
    @FXML private VBox faseTest;
    @FXML private Label contadorRespuestas;
    @FXML private ProgressBar barraRespuestas;
    @FXML private VBox listaPreguntas;
    @FXML private Label mensajeTest;
    @FXML private VBox faseResultado;
    @FXML private VBox tarjetaResultado;
    @FXML private VBox listaCorrecciones;
    @FXML private Button botonReintentar;

    private Leccion leccion;
    private Runnable alVolver = () -> { };
    private List<Pregunta> preguntas = List.of();
    private final Map<Integer, ToggleGroup> respuestas = new LinkedHashMap<>();

    /** Carga la lección (comprobando que no está bloqueada, RN-08) y muestra la fase de lectura. */
    public void mostrar(Leccion seleccionada, Runnable volver) {
        this.alVolver = volver;
        try {
            leccion = servicio.abrirLeccion(Sesion.getUsuario().getIdUsuario(), seleccionada.getIdLeccion());
        } catch (ValidacionException | DataAccessException e) {
            leccion = seleccionada;
            mostrarError(e.getMessage());
            botonIniciarTest.setDisable(true);
        }
        chipNivel.setText(EducacionService.nombreNivel(leccion.getNivel()));
        tituloLeccion.setText(leccion.getTitulo());
        mostrarContenido();
        verFase(faseLectura, "Lectura");
    }

    // ------------------------------------------------------------------ fase 1: lectura

    private void mostrarContenido() {
        contenidoLeccion.getChildren().clear();
        String[] parrafos = leccion.getContenido().strip().split("\\R\\s*\\R|\\\\n\\\\n");
        int palabras = leccion.getContenido().strip().split("\\s+").length;
        datosLeccion.setText("◷  " + Math.max(1, Math.round(palabras / 150.0)) + " min de lectura");
        for (String parrafo : parrafos) {
            if (!parrafo.isBlank()) {
                contenidoLeccion.getChildren().add(parrafo(parrafo.strip()));
            }
        }
    }

    /** Párrafo con los conceptos en mayúsculas resaltados en negrita. */
    private TextFlow parrafo(String texto) {
        TextFlow flujo = new TextFlow();
        flujo.getStyleClass().add("parrafo-leccion");
        Matcher m = CONCEPTO.matcher(texto);
        int desde = 0;
        while (m.find()) {
            if (m.start() > desde) {
                flujo.getChildren().add(texto(texto.substring(desde, m.start()), "texto-leccion"));
            }
            flujo.getChildren().add(texto(m.group(), "concepto-leccion"));
            desde = m.end();
        }
        if (desde < texto.length()) {
            flujo.getChildren().add(texto(texto.substring(desde), "texto-leccion"));
        }
        flujo.prefWidthProperty().bind(contenidoLeccion.widthProperty().subtract(60));
        return flujo;
    }

    private static Text texto(String contenido, String estilo) {
        Text t = new Text(contenido);
        t.getStyleClass().add(estilo);
        return t;
    }

    // ------------------------------------------------------------------ fase 2: test

    @FXML
    private void iniciarTest() {
        ocultarError();
        try {
            preguntas = servicio.preguntasDelTest(Sesion.getUsuario().getIdUsuario(), leccion.getIdLeccion());
        } catch (ValidacionException | DataAccessException e) {
            mostrarError(e.getMessage());
            return;
        }
        respuestas.clear();
        listaPreguntas.getChildren().clear();
        for (int i = 0; i < preguntas.size(); i++) {
            listaPreguntas.getChildren().add(tarjetaPregunta(i + 1, preguntas.get(i)));
        }
        mensajeTest.setVisible(false);
        mensajeTest.setManaged(false);
        actualizarContador();
        verFase(faseTest, "Test");
    }

    private VBox tarjetaPregunta(int numero, Pregunta p) {
        Label enunciado = new Label(numero + ".  " + p.getEnunciado());
        enunciado.getStyleClass().add("pregunta-enunciado");
        enunciado.setWrapText(true);

        ToggleGroup grupo = new ToggleGroup();
        grupo.selectedToggleProperty().addListener((obs, a, b) -> actualizarContador());
        respuestas.put(p.getIdPregunta(), grupo);

        VBox opciones = new VBox(8);
        String[] textos = {p.getOpcionA(), p.getOpcionB(), p.getOpcionC(), p.getOpcionD()};
        for (int i = 0; i < LETRAS.length; i++) {
            RadioButton opcion = new RadioButton(LETRAS[i] + ")  " + textos[i]);
            opcion.setUserData(LETRAS[i]);
            opcion.setToggleGroup(grupo);
            opcion.setWrapText(true);
            opcion.getStyleClass().add("opcion-test");
            opciones.getChildren().add(opcion);
        }
        VBox tarjeta = new VBox(12, enunciado, opciones);
        tarjeta.getStyleClass().add("tarjeta-pregunta");
        return tarjeta;
    }

    private void actualizarContador() {
        long respondidas = respuestas.values().stream().filter(g -> g.getSelectedToggle() != null).count();
        int total = preguntas.size();
        contadorRespuestas.setText("Respondidas " + respondidas + " de " + total);
        barraRespuestas.setProgress(total == 0 ? 0 : (double) respondidas / total);
    }

    @FXML
    private void enviarTest() {
        Map<Integer, Character> elegidas = new HashMap<>();
        respuestas.forEach((idPregunta, grupo) -> {
            if (grupo.getSelectedToggle() != null) {
                elegidas.put(idPregunta, (Character) grupo.getSelectedToggle().getUserData());
            }
        });
        try {
            ResultadoTest resultado = servicio.corregirTest(Sesion.getUsuario().getIdUsuario(),
                    leccion.getIdLeccion(), elegidas, LocalDateTime.now());
            mostrarResultado(resultado);
        } catch (ValidacionException | DataAccessException e) {
            // Falta alguna respuesta, o error al guardar (la transacción se ha deshecho)
            mensajeTest.setText(e.getMessage());
            mensajeTest.setVisible(true);
            mensajeTest.setManaged(true);
        }
    }

    // ------------------------------------------------------------------ fase 3: resultado

    private void mostrarResultado(ResultadoTest r) {
        tarjetaResultado.getChildren().clear();
        tarjetaResultado.getStyleClass().removeAll("resultado-aprobado", "resultado-suspenso");
        tarjetaResultado.getStyleClass().add(r.aprobado() ? "resultado-aprobado" : "resultado-suspenso");

        Label nota = new Label(Formato.porcentaje(r.nota(), false).replace(",0 %", " %"));
        nota.getStyleClass().add("resultado-nota");
        Label titulo = new Label(r.aprobado() ? "¡Enhorabuena! Has superado la lección" : "Todavía no has superado la lección");
        titulo.getStyleClass().add("tarjeta-titulo");
        Label detalle = new Label(r.aciertos() + " de " + r.total() + " respuestas correctas · necesitas un 70 % para aprobar");
        detalle.getStyleClass().add("nota");
        VBox textos = new VBox(4, titulo, detalle);
        HBox cabecera = new HBox(20, nota, textos);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        tarjetaResultado.getChildren().add(cabecera);

        String mejor = "Tu mejor nota en esta lección: " + Formato.porcentaje(r.mejorNota(), false).replace(",0 %", " %");
        // La felicitación por mejorar la nota solo tiene sentido si además se aprueba
        if (r.aprobado() && r.nuevaMejorNota()) {
            mejor += " (¡nueva mejor nota!)";
        }
        tarjetaResultado.getChildren().add(linea(mejor, "nota"));
        if (!r.aprobado()) {
            // E-01: no se supera la lección ni sube el medidor; se invita a repasar
            tarjetaResultado.getChildren().add(linea("Repasa el contenido y vuelve a intentarlo: "
                    + "tu progreso no cambia hasta que apruebes.", "mensaje-aviso"));
        }
        if (r.nivelDesbloqueado() != null) {
            tarjetaResultado.getChildren().add(linea("★ Has desbloqueado el nivel "
                    + EducacionService.nombreNivel(r.nivelDesbloqueado()) + ".", "aviso-desbloqueo"));
        }

        listaCorrecciones.getChildren().clear();
        int numero = 1;
        for (ResultadoTest.Correccion c : r.correcciones()) {
            listaCorrecciones.getChildren().add(tarjetaCorreccion(numero++, c));
        }
        botonReintentar.setText(r.aprobado() ? "Repetir el test" : "Reintentar el test");
        verFase(faseResultado, r.aprobado() ? "Superada" : "No superada");
    }

    private VBox tarjetaCorreccion(int numero, ResultadoTest.Correccion c) {
        Pregunta p = c.pregunta();
        Label icono = new Label(c.acierto() ? "✓" : "✕");
        icono.getStyleClass().add(c.acierto() ? "correccion-bien" : "correccion-mal");
        Label enunciado = new Label(numero + ".  " + p.getEnunciado());
        enunciado.getStyleClass().add("pregunta-enunciado");
        enunciado.setWrapText(true);
        HBox.setHgrow(enunciado, Priority.ALWAYS);
        HBox cabecera = new HBox(10, icono, enunciado);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(6, cabecera);
        tarjeta.getChildren().add(linea("Tu respuesta: " + c.elegida() + ") " + opcion(p, c.elegida()),
                c.acierto() ? "texto-positivo" : "texto-negativo"));
        if (!c.acierto()) {
            tarjeta.getChildren().add(linea("Respuesta correcta: " + p.getRespuestaCorrecta() + ") "
                    + opcion(p, p.getRespuestaCorrecta()), "texto-positivo"));
        }
        tarjeta.getStyleClass().add("tarjeta-pregunta");
        return tarjeta;
    }

    // ------------------------------------------------------------------ navegación

    @FXML
    private void volverALectura() {
        ocultarError();
        verFase(faseLectura, "Lectura");
    }

    /** Volver a la ruta. Si el test no se ha enviado, no se guarda ningún progreso (FA-01). */
    @FXML
    private void volver() {
        alVolver.run();
    }

    private void verFase(VBox fase, String nombre) {
        for (VBox f : List.of(faseLectura, faseTest, faseResultado)) {
            f.setVisible(f == fase);
            f.setManaged(f == fase);
        }
        chipFase.setText(nombre);
        // Tras el siguiente pulso de diseño: si se hace antes, el ScrollPane aún tiene la altura de la fase anterior
        Platform.runLater(() -> desplazamiento.setVvalue(0));
    }

    private static String opcion(Pregunta p, char letra) {
        return switch (Character.toUpperCase(letra)) {
            case 'A' -> p.getOpcionA();
            case 'B' -> p.getOpcionB();
            case 'C' -> p.getOpcionC();
            default -> p.getOpcionD();
        };
    }

    private static Label linea(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setWrapText(true);
        return l;
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
