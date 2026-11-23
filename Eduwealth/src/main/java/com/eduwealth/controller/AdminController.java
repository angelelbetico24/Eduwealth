package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.AdminService;
import com.eduwealth.service.DivisaService;
import com.eduwealth.service.EducacionService;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Panel de administración (CU-12): mercado y divisas, brokers y lecciones. Toda la validación
 * y la comprobación del rol ADMIN están en {@link AdminService}.
 */
public class AdminController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ESTILO_ERROR = "campo-error";

    private final AdminService admin = FabricaServicios.admin();
    private final DivisaService divisaService = FabricaServicios.divisas();

    @FXML private Label mensaje;
    // Mercado y divisas
    @FXML private GridPane tablaDivisas;
    @FXML private GridPane tablaActivos;
    // Brokers
    @FXML private ListView<Broker> listaBrokers;
    @FXML private Label tituloBroker;
    @FXML private TextField brokerNombre;
    @FXML private TextArea brokerDescripcion;
    @FXML private TextField brokerComisiones;
    @FXML private TextField brokerComisionMinima;
    @FXML private CheckBox tipoAccion;
    @FXML private CheckBox tipoEtf;
    @FXML private CheckBox tipoFondo;
    @FXML private CheckBox tipoCripto;
    @FXML private ComboBox<Nivel> brokerNivel;
    @FXML private TextField brokerRegulacion;
    @FXML private TextField brokerUrl;
    @FXML private Label errorBroker;
    @FXML private Button botonEliminarBroker;
    // Lecciones
    @FXML private ListView<Leccion> listaLecciones;
    @FXML private Label tituloLeccion;
    @FXML private TextField leccionTitulo;
    @FXML private ComboBox<Nivel> leccionNivel;
    @FXML private TextArea leccionContenido;
    @FXML private Label errorLeccion;
    @FXML private Button botonEliminarLeccion;
    @FXML private VBox tarjetaPreguntas;
    @FXML private Label tituloPreguntas;
    @FXML private ListView<Pregunta> listaPreguntas;
    @FXML private TextField preguntaEnunciado;
    @FXML private TextField opcionA;
    @FXML private TextField opcionB;
    @FXML private TextField opcionC;
    @FXML private TextField opcionD;
    @FXML private ComboBox<Character> preguntaCorrecta;
    @FXML private Button botonEliminarPregunta;
    @FXML private Label errorPregunta;

    private Broker brokerEditado;
    private Leccion leccionEditada;
    private Pregunta preguntaEditada;
    private Map<CheckBox, TipoActivo> casillasTipo;

    @FXML
    private void initialize() {
        casillasTipo = Map.of(tipoAccion, TipoActivo.ACCION, tipoEtf, TipoActivo.ETF,
                tipoFondo, TipoActivo.FONDO, tipoCripto, TipoActivo.CRIPTO);
        StringConverter<Nivel> nombreNivel = new StringConverter<>() {
            @Override
            public String toString(Nivel n) {
                return n == null ? "" : EducacionService.nombreNivel(n);
            }

            @Override
            public Nivel fromString(String s) {
                return null;
            }
        };
        for (ComboBox<Nivel> combo : List.of(brokerNivel, leccionNivel)) {
            combo.getItems().setAll(Nivel.values());
            combo.setConverter(nombreNivel);
        }
        preguntaCorrecta.getItems().setAll('A', 'B', 'C', 'D');

        listaBrokers.setCellFactory(l -> celda(Broker::getNombre));
        listaBrokers.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                editarBroker(b);
            }
        });
        listaLecciones.setCellFactory(l -> celda(x -> "[" + EducacionService.nombreNivel(x.getNivel()) + "]  " + x.getTitulo()));
        listaLecciones.getSelectionModel().selectedItemProperty().addListener((o, a, l) -> {
            if (l != null) {
                editarLeccion(l);
            }
        });
        listaPreguntas.setCellFactory(l -> celda(p -> p.getEnunciado() + "  (" + p.getRespuestaCorrecta() + ")"));
        listaPreguntas.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                editarPregunta(p);
            }
        });

        try {
            admin.comprobarAdmin(Sesion.getUsuario());
            cargarMercado();
            cargarBrokers(null);
            cargarLecciones(null);
        } catch (ValidacionException | DataAccessException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    // ================================================================== mercado y divisas

    private void cargarMercado() {
        Usuario u = Sesion.getUsuario();
        LocalDateTime ahora = LocalDateTime.now();

        tablaDivisas.getChildren().clear();
        cabecera(tablaDivisas, "Divisa", "Unidades por 1 €", "Actualizado", "");
        int fila = 1;
        for (Divisa d : admin.divisas(u)) {
            boolean pivote = AdminService.DIVISA_PIVOTE.equals(d.getCodigo());
            TextField valor = new TextField(texto(d.getTipoCambioBase()));
            valor.setDisable(pivote);
            Label fecha = new Label(d.getFechaActualizacion() == null ? "—"
                    : d.getFechaActualizacion().format(FECHA)
                    + (!pivote && divisaService.estaDesactualizada(d, ahora) ? "  ⚠ desactualizado" : ""));
            fecha.getStyleClass().add(!pivote && divisaService.estaDesactualizada(d, ahora) ? "mensaje-aviso" : "nota");
            Button guardar = new Button(pivote ? "Pivote" : "Guardar");
            guardar.getStyleClass().add("boton-secundario");
            guardar.setDisable(pivote);
            guardar.setOnAction(e -> ejecutar(valor, () -> {
                admin.actualizarTipoCambio(u, d, valor.getText(), LocalDateTime.now());
                cargarMercado();
                mostrarMensaje("Tipo de cambio de " + d.getCodigo() + " actualizado.", true);
            }));
            tablaDivisas.addRow(fila++, new Label(d.getCodigo() + " · " + d.getNombre()), valor, fecha, guardar);
        }

        Map<Integer, Divisa> divisas = admin.divisas(u).stream()
                .collect(Collectors.toMap(Divisa::getIdDivisa, Function.identity()));
        tablaActivos.getChildren().clear();
        cabecera(tablaActivos, "Activo", "Tipo", "Precio", "Divisa", "Actualizado", "");
        fila = 1;
        for (Activo a : admin.activos(u)) {
            Label nombre = new Label(a.getTicker() + " · " + a.getNombre());
            nombre.getStyleClass().add("tabla-ticker");
            TextField precio = new TextField(texto(a.getPrecioActual()));
            precio.setPrefWidth(130);
            Label fecha = new Label(a.getFechaActualizacion() == null ? "—" : a.getFechaActualizacion().format(FECHA));
            fecha.getStyleClass().add("nota");
            Button guardar = new Button("Guardar");
            guardar.getStyleClass().add("boton-secundario");
            guardar.setOnAction(e -> ejecutar(precio, () -> {
                admin.actualizarPrecio(u, a, precio.getText(), LocalDateTime.now());
                cargarMercado();
                mostrarMensaje("Precio de " + a.getTicker() + " actualizado.", true);
            }));
            tablaActivos.addRow(fila++, nombre, new Label(SimuladorController.nombreTipo(a.getTipoActivo())), precio,
                    new Label(divisas.get(a.getIdDivisa()).getCodigo()), fecha, guardar);
        }
    }

    @FXML
    private void simularDia() {
        if (!confirmar("Simular un día de mercado", "¿Mover los precios de todos los activos?",
                "Se aplicará un día de variación aleatoria a cada activo según su tipo. Afecta a todos los usuarios.",
                "Simular")) {
            return;
        }
        ejecutar(null, () -> {
            admin.simularDiaDeMercado(Sesion.getUsuario(), LocalDateTime.now());
            cargarMercado();
            mostrarMensaje("Día de mercado simulado: precios actualizados.", true);
        });
    }

    // ================================================================== brokers

    private void cargarBrokers(Integer seleccionar) {
        listaBrokers.getItems().setAll(admin.brokers(Sesion.getUsuario()));
        listaBrokers.getItems().stream().filter(b -> seleccionar != null && b.getIdBroker() == seleccionar)
                .findFirst().ifPresentOrElse(b -> listaBrokers.getSelectionModel().select(b), this::nuevoBroker);
    }

    @FXML
    private void nuevoBroker() {
        listaBrokers.getSelectionModel().clearSelection();
        editarBroker(new Broker(0, "", "", "", BigDecimal.ZERO, EnumSet.noneOf(TipoActivo.class),
                Nivel.PRINCIPIANTE, "", "https://"));
    }

    private void editarBroker(Broker b) {
        brokerEditado = b;
        limpiarErrores(errorBroker, brokerNombre, brokerComisionMinima, brokerUrl, brokerNivel);
        tituloBroker.setText(b.getIdBroker() == 0 ? "Nuevo broker" : "Editar " + b.getNombre());
        brokerNombre.setText(b.getNombre());
        brokerDescripcion.setText(b.getDescripcion());
        brokerComisiones.setText(b.getComisionesInfo());
        brokerComisionMinima.setText(b.getComisionMinima() == null ? "" : texto(b.getComisionMinima()));
        casillasTipo.forEach((casilla, tipo) -> casilla.setSelected(b.ofrece(tipo)));
        brokerNivel.setValue(b.getNivelRecomendado());
        brokerRegulacion.setText(b.getRegulacion());
        brokerUrl.setText(b.getUrlReferido());
        botonEliminarBroker.setDisable(b.getIdBroker() == 0);
    }

    @FXML
    private void guardarBroker() {
        limpiarErrores(errorBroker, brokerNombre, brokerComisionMinima, brokerUrl, brokerNivel);
        Set<TipoActivo> tipos = EnumSet.noneOf(TipoActivo.class);
        casillasTipo.forEach((casilla, tipo) -> {
            if (casilla.isSelected()) {
                tipos.add(tipo);
            }
        });
        Map<String, Node> campos = Map.of("nombre", brokerNombre, "comision", brokerComisionMinima, "url", brokerUrl,
                "nivel", brokerNivel, "descripcion", brokerDescripcion, "comisiones", brokerComisiones,
                "regulacion", brokerRegulacion);
        try {
            Broker guardado = admin.guardarBroker(Sesion.getUsuario(), brokerEditado.getIdBroker(), brokerNombre.getText(),
                    brokerDescripcion.getText(), brokerComisiones.getText(), brokerComisionMinima.getText(), tipos,
                    brokerNivel.getValue(), brokerRegulacion.getText(), brokerUrl.getText());
            cargarBrokers(guardado.getIdBroker());
            mostrarMensaje("Broker «" + guardado.getNombre() + "» guardado.", true);
        } catch (ValidacionException e) {
            marcarError(errorBroker, e, campos);
        } catch (DataAccessException e) {
            mostrarError(errorBroker, e.getMessage());
        }
    }

    @FXML
    private void eliminarBroker() {
        if (brokerEditado == null || brokerEditado.getIdBroker() == 0
                || !confirmar("Eliminar broker", "¿Eliminar «" + brokerEditado.getNombre() + "»?",
                "Dejará de aparecer en el directorio de todos los usuarios.", "Eliminar")) {
            return;
        }
        ejecutar(null, () -> {
            admin.eliminarBroker(Sesion.getUsuario(), brokerEditado.getIdBroker());
            mostrarMensaje("Broker «" + brokerEditado.getNombre() + "» eliminado.", true);
            cargarBrokers(null);
        });
    }

    // ================================================================== lecciones y preguntas

    private void cargarLecciones(Integer seleccionar) {
        listaLecciones.getItems().setAll(admin.lecciones(Sesion.getUsuario()));
        listaLecciones.getItems().stream().filter(l -> seleccionar != null && l.getIdLeccion() == seleccionar)
                .findFirst().ifPresentOrElse(l -> listaLecciones.getSelectionModel().select(l), () -> {
                    if (listaLecciones.getItems().isEmpty()) {
                        nuevaLeccion();
                    } else {
                        listaLecciones.getSelectionModel().selectFirst();
                    }
                });
    }

    @FXML
    private void nuevaLeccion() {
        listaLecciones.getSelectionModel().clearSelection();
        editarLeccion(new Leccion(0, "", "", Nivel.PRINCIPIANTE));
    }

    private void editarLeccion(Leccion l) {
        leccionEditada = l;
        limpiarErrores(errorLeccion, leccionTitulo, leccionContenido, leccionNivel);
        tituloLeccion.setText(l.getIdLeccion() == 0 ? "Nueva lección" : "Editar lección");
        leccionTitulo.setText(l.getTitulo());
        leccionNivel.setValue(l.getNivel());
        leccionContenido.setText(l.getContenido());
        botonEliminarLeccion.setDisable(l.getIdLeccion() == 0);
        // Las preguntas solo se pueden añadir a una lección ya guardada
        tarjetaPreguntas.setDisable(l.getIdLeccion() == 0);
        tituloPreguntas.setText(l.getIdLeccion() == 0 ? "Preguntas (guarda antes la lección)" : "Preguntas del test");
        cargarPreguntas(null);
    }

    @FXML
    private void guardarLeccion() {
        limpiarErrores(errorLeccion, leccionTitulo, leccionContenido, leccionNivel);
        try {
            Leccion guardada = admin.guardarLeccion(Sesion.getUsuario(), leccionEditada.getIdLeccion(),
                    leccionTitulo.getText(), leccionNivel.getValue(), leccionContenido.getText());
            cargarLecciones(guardada.getIdLeccion());
            mostrarMensaje("Lección «" + guardada.getTitulo() + "» guardada.", true);
        } catch (ValidacionException e) {
            marcarError(errorLeccion, e, Map.of("titulo", leccionTitulo, "contenido", leccionContenido,
                    "nivel", leccionNivel));
        } catch (DataAccessException e) {
            mostrarError(errorLeccion, e.getMessage());
        }
    }

    @FXML
    private void eliminarLeccion() {
        if (leccionEditada == null || leccionEditada.getIdLeccion() == 0
                || !confirmar("Eliminar lección", "¿Eliminar «" + leccionEditada.getTitulo() + "»?",
                "Se borrarán también sus preguntas y el progreso de todos los usuarios en esta lección. "
                        + "Esta acción no se puede deshacer.", "Eliminar")) {
            return;
        }
        ejecutar(null, () -> {
            admin.eliminarLeccion(Sesion.getUsuario(), leccionEditada.getIdLeccion());
            mostrarMensaje("Lección «" + leccionEditada.getTitulo() + "» eliminada.", true);
            cargarLecciones(null);
        });
    }

    private void cargarPreguntas(Integer seleccionar) {
        if (leccionEditada == null || leccionEditada.getIdLeccion() == 0) {
            listaPreguntas.getItems().clear();
            nuevaPregunta();
            return;
        }
        listaPreguntas.getItems().setAll(admin.preguntas(Sesion.getUsuario(), leccionEditada.getIdLeccion()));
        listaPreguntas.getItems().stream().filter(p -> seleccionar != null && p.getIdPregunta() == seleccionar)
                .findFirst().ifPresentOrElse(p -> listaPreguntas.getSelectionModel().select(p), this::nuevaPregunta);
    }

    @FXML
    private void nuevaPregunta() {
        listaPreguntas.getSelectionModel().clearSelection();
        int idLeccion = leccionEditada == null ? 0 : leccionEditada.getIdLeccion();
        editarPregunta(new Pregunta(0, idLeccion, "", "", "", "", "", 'A'));
    }

    private void editarPregunta(Pregunta p) {
        preguntaEditada = p;
        limpiarErrores(errorPregunta, preguntaEnunciado, opcionA, opcionB, opcionC, opcionD, preguntaCorrecta);
        preguntaEnunciado.setText(p.getEnunciado());
        opcionA.setText(p.getOpcionA());
        opcionB.setText(p.getOpcionB());
        opcionC.setText(p.getOpcionC());
        opcionD.setText(p.getOpcionD());
        preguntaCorrecta.setValue(p.getRespuestaCorrecta());
        botonEliminarPregunta.setDisable(p.getIdPregunta() == 0);
    }

    @FXML
    private void guardarPregunta() {
        limpiarErrores(errorPregunta, preguntaEnunciado, opcionA, opcionB, opcionC, opcionD, preguntaCorrecta);
        try {
            Pregunta guardada = admin.guardarPregunta(Sesion.getUsuario(), preguntaEditada.getIdPregunta(),
                    leccionEditada.getIdLeccion(), preguntaEnunciado.getText(), opcionA.getText(), opcionB.getText(),
                    opcionC.getText(), opcionD.getText(), preguntaCorrecta.getValue());
            cargarPreguntas(guardada.getIdPregunta());
            mostrarMensaje("Pregunta guardada.", true);
        } catch (ValidacionException e) {
            marcarError(errorPregunta, e, Map.of("enunciado", preguntaEnunciado, "opcionA", opcionA, "opcionB", opcionB,
                    "opcionC", opcionC, "opcionD", opcionD, "correcta", preguntaCorrecta));
        } catch (DataAccessException e) {
            mostrarError(errorPregunta, e.getMessage());
        }
    }

    @FXML
    private void eliminarPregunta() {
        if (preguntaEditada == null || preguntaEditada.getIdPregunta() == 0
                || !confirmar("Eliminar pregunta", "¿Eliminar esta pregunta?", preguntaEditada.getEnunciado(), "Eliminar")) {
            return;
        }
        ejecutar(null, () -> {
            admin.eliminarPregunta(Sesion.getUsuario(), preguntaEditada.getIdPregunta());
            mostrarMensaje("Pregunta eliminada.", true);
            cargarPreguntas(null);
        });
    }

    // ================================================================== auxiliares

    /** Ejecuta una acción del servicio mostrando sus errores; si indica un campo, lo marca en rojo. */
    private void ejecutar(TextInputControl campo, Runnable accion) {
        if (campo != null) {
            campo.getStyleClass().remove(ESTILO_ERROR);
        }
        try {
            accion.run();
        } catch (ValidacionException e) {
            mostrarMensaje(e.getMessage(), false);
            if (campo != null && e.getCampo() != null) {
                campo.getStyleClass().add(ESTILO_ERROR);
            }
        } catch (DataAccessException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private void marcarError(Label etiqueta, ValidacionException e, Map<String, Node> campos) {
        mostrarError(etiqueta, e.getMessage());
        Node campo = e.getCampo() == null ? null : campos.get(e.getCampo());
        if (campo != null) {
            campo.getStyleClass().add(ESTILO_ERROR);
            campo.requestFocus();
        }
    }

    private static void limpiarErrores(Label etiqueta, Node... campos) {
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        for (Node c : campos) {
            c.getStyleClass().remove(ESTILO_ERROR);
        }
    }

    private static void mostrarError(Label etiqueta, String texto) {
        etiqueta.setText(texto);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        mensaje.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private boolean confirmar(String titulo, String cabecera, String texto, String accion) {
        ButtonType aceptar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, aceptar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecera);
        alerta.initOwner(mensaje.getScene().getWindow());
        alerta.getDialogPane().getStylesheets().addAll(mensaje.getScene().getStylesheets());
        return alerta.showAndWait().orElse(null) == aceptar;
    }

    private static void cabecera(GridPane tabla, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            Label l = new Label(titulos[i]);
            l.getStyleClass().add("nota");
            tabla.add(l, i, 0);
        }
        GridPane.setHgrow(tabla.getChildren().get(0), Priority.ALWAYS);
    }

    private static <T> ListCell<T> celda(Function<T, String> texto) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vacia) {
                super.updateItem(item, vacia);
                setText(vacia || item == null ? null : texto.apply(item));
                setAlignment(Pos.CENTER_LEFT);
            }
        };
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
