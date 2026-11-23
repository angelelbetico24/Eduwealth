package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.service.CategoriaPresupuesto;
import com.eduwealth.service.PresupuestoService;
import com.eduwealth.service.ResumenPresupuestos;
import com.eduwealth.service.ValidacionException;
import com.eduwealth.util.Formato;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ventana para definir, ajustar o quitar los límites de todas las categorías de un mes
 * (CU-10, C-27). Solo gestiona la interfaz: valida y guarda {@link PresupuestoService#guardarLimites}.
 */
final class DialogoLimites {

    private static final String ESTILO_ERROR = "campo-error";

    private final PresupuestoService servicio;
    private final int idUsuario;
    private final YearMonth mes;
    private final String simbolo;
    private final Map<CategoriaGasto, TextField> campos = new LinkedHashMap<>();
    private final Label mensaje = new Label();
    private Dialog<ButtonType> dialogo;

    DialogoLimites(PresupuestoService servicio, int idUsuario, YearMonth mes, String simbolo) {
        this.servicio = servicio;
        this.idUsuario = idUsuario;
        this.mes = mes;
        this.simbolo = simbolo;
    }

    /** Muestra la ventana y devuelve si se han guardado cambios. */
    boolean mostrar(Window propietaria, List<String> estilos, List<CategoriaGasto> categorias,
                    ResumenPresupuestos actual) {
        dialogo = new Dialog<>();
        dialogo.initOwner(propietaria);
        dialogo.setTitle("Gestionar límites");
        dialogo.getDialogPane().getStylesheets().addAll(estilos);
        dialogo.getDialogPane().getStyleClass().add("dialogo-limites");

        ButtonType guardar = new ButtonType("Guardar límites", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(guardar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().setContent(contenido(categorias, actual));

        // Si hay errores, la ventana no se cierra: se marca el campo y se explica el motivo
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (!intentarGuardar()) {
                evento.consume();
            }
        });
        return dialogo.showAndWait().orElse(null) == guardar;
    }

    private VBox contenido(List<CategoriaGasto> categorias, ResumenPresupuestos actual) {
        Label titulo = new Label("Límites de " + PresupuestoService.nombreMes(mes));
        titulo.getStyleClass().add("tarjeta-titulo");
        Label ayuda = new Label("Fija cuánto quieres gastar como máximo en cada categoría. "
                + "Deja el campo vacío para no presupuestarla.");
        ayuda.getStyleClass().add("nota");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(Region.USE_PREF_SIZE); // que no se recorte al aparecer el mensaje de error

        Button copiar = new Button("⟲  Copiar mes anterior");
        copiar.getStyleClass().add("boton-secundario");
        copiar.setMinWidth(Region.USE_PREF_SIZE);
        copiar.setOnAction(e -> copiarMesAnterior());

        Map<Integer, CategoriaPresupuesto> porCategoria = actual.categorias().stream()
                .collect(Collectors.toMap(CategoriaPresupuesto::idCategoria, Function.identity()));
        GridPane tabla = new GridPane();
        tabla.setHgap(12);
        tabla.setVgap(10);
        int fila = 0;
        for (CategoriaGasto categoria : categorias) {
            CategoriaPresupuesto existente = porCategoria.get(categoria.getIdCategoria());
            Label nombre = new Label(categoria.getNombre());
            nombre.getStyleClass().add("etiqueta-campo");
            VBox textos = new VBox(2, nombre);
            if (existente != null && existente.gastado().signum() > 0) {
                Label gastado = new Label("Gastado: " + Formato.moneda(existente.gastado(), simbolo));
                gastado.getStyleClass().add("nota");
                textos.getChildren().add(gastado);
            }
            GridPane.setHgrow(textos, Priority.ALWAYS);
            GridPane.setValignment(textos, VPos.CENTER);
            textos.setAlignment(Pos.CENTER_LEFT);

            TextField campo = new TextField(existente == null ? "" : texto(existente.limite()));
            campo.setPromptText("Sin límite");
            campo.setPrefWidth(140);
            campos.put(categoria, campo);
            Label moneda = new Label(simbolo);
            moneda.getStyleClass().add("nota");

            tabla.addRow(fila++, textos, campo, moneda);
        }

        mensaje.getStyleClass().add("mensaje-error");
        mensaje.setWrapText(true);
        mensaje.setMinHeight(Region.USE_PREF_SIZE);
        ocultarMensaje();

        VBox textosCabecera = new VBox(4, titulo, ayuda);
        HBox.setHgrow(textosCabecera, Priority.ALWAYS); // el texto ocupa el ancho libre y se ajusta en varias líneas
        HBox cabecera = new HBox(16, textosCabecera, copiar);
        cabecera.setAlignment(Pos.TOP_LEFT);
        VBox contenido = new VBox(16, cabecera, tabla, mensaje);
        contenido.setPrefWidth(520);
        return contenido;
    }

    private boolean intentarGuardar() {
        campos.values().forEach(c -> c.getStyleClass().remove(ESTILO_ERROR));
        ocultarMensaje();
        Map<CategoriaGasto, String> limites = new LinkedHashMap<>();
        campos.forEach((categoria, campo) -> limites.put(categoria, campo.getText()));
        try {
            servicio.guardarLimites(idUsuario, mes, limites);
            return true;
        } catch (ValidacionException e) {
            mostrarMensaje(e.getMessage());
            campoDe(e.getCampo()).ifPresent(campo -> {
                campo.getStyleClass().add(ESTILO_ERROR);
                campo.requestFocus();
            });
        } catch (DataAccessException e) {
            mostrarMensaje(e.getMessage()); // la transacción se ha deshecho: no se ha guardado nada
        }
        return false;
    }

    /** Rellena los campos con los límites del mes anterior (no se guarda hasta pulsar «Guardar»). */
    private void copiarMesAnterior() {
        YearMonth anterior = mes.minusMonths(1);
        try {
            ResumenPresupuestos previo = servicio.resumenDelMes(idUsuario, anterior);
            if (previo.vacio()) {
                mostrarMensaje("No había presupuestos en " + PresupuestoService.nombreMes(anterior) + ".");
                return;
            }
            Map<Integer, BigDecimal> limites = previo.categorias().stream()
                    .collect(Collectors.toMap(CategoriaPresupuesto::idCategoria, CategoriaPresupuesto::limite));
            campos.forEach((categoria, campo) -> {
                BigDecimal limite = limites.get(categoria.getIdCategoria());
                campo.setText(limite == null ? "" : texto(limite));
            });
            ocultarMensaje();
        } catch (DataAccessException e) {
            mostrarMensaje(e.getMessage());
        }
    }

    private Optional<TextField> campoDe(String campo) {
        if (campo == null || !campo.startsWith("limite-")) {
            return Optional.empty();
        }
        int id = Integer.parseInt(campo.substring("limite-".length()));
        return campos.entrySet().stream()
                .filter(e -> e.getKey().getIdCategoria() == id)
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static String texto(BigDecimal importe) {
        return importe.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private void mostrarMensaje(String texto) {
        mensaje.setText(texto);
        mensaje.setVisible(true);
        mensaje.setManaged(true);
        // La ventana crece para que el mensaje no empuje los botones fuera del borde
        if (dialogo != null && dialogo.isShowing()) {
            dialogo.getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private void ocultarMensaje() {
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }
}
