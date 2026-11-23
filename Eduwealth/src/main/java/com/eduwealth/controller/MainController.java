package com.eduwealth.controller;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Usuario;
import com.eduwealth.service.FabricaServicios;
import com.eduwealth.util.Navegador;
import com.eduwealth.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estructura principal: menú lateral y área de contenido donde se cargan los módulos.
 * Cumple RNF-04: cualquier módulo queda a un clic desde el menú.
 */
public class MainController {

    private static final String ESTILO_ACTIVO = "nav-activo";

    @FXML
    private StackPane contenido;
    @FXML
    private Button navInicio;
    @FXML
    private Button navSimulador;
    @FXML
    private Button navPresupuestos;
    @FXML
    private Button navEducacion;
    @FXML
    private Button navBrokers;
    @FXML
    private Button navConfiguracion;
    @FXML
    private Label iniciales;
    @FXML
    private Label nombreUsuario;
    @FXML
    private Label tipoCuenta;
    @FXML
    private Label divisaBase;

    private Map<Seccion, Button> botones;

    @FXML
    private void initialize() {
        botones = new EnumMap<>(Map.of(
                Seccion.INICIO, navInicio,
                Seccion.SIMULADOR, navSimulador,
                Seccion.PRESUPUESTOS, navPresupuestos,
                Seccion.EDUCACION, navEducacion,
                Seccion.BROKERS, navBrokers,
                Seccion.CONFIGURACION, navConfiguracion));

        Usuario usuario = Sesion.getUsuario();
        nombreUsuario.setText(usuario.getNombre());
        tipoCuenta.setText(usuario.esAdmin() ? "Administrador" : "Cuenta de aprendizaje");
        iniciales.setText(calcularIniciales(usuario.getNombre()));
        try {
            FabricaServicios.divisas().buscarPorId(usuario.getIdDivisaBase())
                    .ifPresent(d -> divisaBase.setText("Divisa base: " + d.getCodigo()));
        } catch (DataAccessException e) {
            divisaBase.setText("");
        }

        actualizarMercado();
        mostrar(Seccion.INICIO);
    }

    /**
     * C-26: al entrar, los precios avanzan según los días transcurridos desde la última vez.
     * Si falla, se sigue con los precios anteriores: no debe impedir usar la aplicación.
     */
    private void actualizarMercado() {
        try {
            FabricaServicios.mercado().actualizarPrecios(LocalDateTime.now());
        } catch (DataAccessException e) {
            System.err.println("No se pudieron actualizar los precios: " + e.getMessage());
        }
    }

    /** Carga la sección en el área de contenido y marca su botón como activo. */
    public void mostrar(Seccion seccion) {
        botones.values().forEach(b -> b.getStyleClass().remove(ESTILO_ACTIVO));
        botones.get(seccion).getStyleClass().add(ESTILO_ACTIVO);

        if (seccion.estaDisponible()) {
            Navegador.Vista<Object> vista = Navegador.cargar(seccion.getVista());
            if (vista.controlador() instanceof InicioController inicio) {
                inicio.setNavegacion(this::mostrar);
            } else if (vista.controlador() instanceof EducacionController educacion) {
                educacion.setAbrirLeccion(this::mostrarLeccion);
            }
            contenido.getChildren().setAll(vista.raiz());
        } else {
            Navegador.Vista<ProximamenteController> vista = Navegador.cargar("proximamente");
            vista.controlador().mostrar(seccion);
            contenido.getChildren().setAll(vista.raiz());
        }
    }

    /** CU-08: abre una lección dentro del área de contenido; al volver se recarga la ruta de aprendizaje. */
    private void mostrarLeccion(Leccion leccion) {
        Navegador.Vista<LeccionController> vista = Navegador.cargar("leccion");
        vista.controlador().mostrar(leccion, () -> mostrar(Seccion.EDUCACION));
        contenido.getChildren().setAll(vista.raiz());
    }

    @FXML
    private void irInicio() {
        mostrar(Seccion.INICIO);
    }

    @FXML
    private void irSimulador() {
        mostrar(Seccion.SIMULADOR);
    }

    @FXML
    private void irPresupuestos() {
        mostrar(Seccion.PRESUPUESTOS);
    }

    @FXML
    private void irEducacion() {
        mostrar(Seccion.EDUCACION);
    }

    @FXML
    private void irBrokers() {
        mostrar(Seccion.BROKERS);
    }

    @FXML
    private void irConfiguracion() {
        mostrar(Seccion.CONFIGURACION);
    }

    @FXML
    private void cerrarSesion() {
        Sesion.cerrar();
        Navegador.ir("login");
    }

    /** "Ángel Muñoz López" → "ÁM". */
    static String calcularIniciales(String nombre) {
        return Arrays.stream(nombre.trim().split("\\s+"))
                .limit(2)
                .map(p -> p.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
}
