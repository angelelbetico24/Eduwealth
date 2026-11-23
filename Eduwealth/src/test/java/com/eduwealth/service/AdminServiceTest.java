package com.eduwealth.service;

import com.eduwealth.model.Activo;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.Rol;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-12: administración reservada al rol ADMIN, con validación de cada dato. */
class AdminServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 12, 0);
    private static final Usuario ADMIN = new Usuario(1, "Admin", "admin@x.com", "h", Rol.ADMIN, BigDecimal.TEN, 1, null);
    private static final Usuario ALUMNO = new Usuario(2, "Ana", "ana@x.com", "h", Rol.USUARIO, BigDecimal.TEN, 1, null);
    private static final Set<TipoActivo> ACCIONES = EnumSet.of(TipoActivo.ACCION);

    private DaosEnMemoria.Divisas divisas;
    private DaosEnMemoria.Activos activos;
    private DaosEnMemoria.Brokers brokers;
    private DaosEnMemoria.Lecciones lecciones;
    private DaosEnMemoria.Preguntas preguntas;
    private AdminService admin;

    @BeforeEach
    void preparar() {
        divisas = new DaosEnMemoria.Divisas()
                .con(new Divisa(1, "EUR", "Euro", "€", new BigDecimal("1.0000"), AHORA.minusDays(9)))
                .con(new Divisa(2, "USD", "Dólar", "$", new BigDecimal("1.0850"), AHORA.minusDays(9)));
        activos = new DaosEnMemoria.Activos()
                .con(new Activo(1, "SAN", "Santander", TipoActivo.ACCION, new BigDecimal("4.0000"), 1, AHORA.minusDays(1)));
        brokers = new DaosEnMemoria.Brokers();
        lecciones = new DaosEnMemoria.Lecciones();
        preguntas = new DaosEnMemoria.Preguntas();
        DaosEnMemoria.Transacciones tx = new DaosEnMemoria.Transacciones();
        admin = new AdminService(divisas, activos, brokers, lecciones, preguntas, tx,
                new MercadoService(activos, tx, new Random(7)), new BrokerService(brokers));
    }

    @Test
    void soloElAdministradorPuedeUsarLaAdministracion() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> admin.divisas(ALUMNO));
        assertTrue(e.getMessage().contains("administrador"));
        assertThrows(ValidacionException.class, () -> admin.simularDiaDeMercado(ALUMNO, AHORA));
        assertThrows(ValidacionException.class, () -> admin.eliminarLeccion(null, 1));
    }

    @Test
    void actualizarTipoDeCambioYSuFecha() {
        Divisa usd = divisas.porId.get(2);
        assertEquals(new BigDecimal("1.1000"), admin.actualizarTipoCambio(ADMIN, usd, "1,1", AHORA));
        assertEquals(new BigDecimal("1.1000"), usd.getTipoCambioBase());
        assertEquals(AHORA, usd.getFechaActualizacion()); // deja de estar desactualizado (C-08)

        assertEquals("tipo-2", campo(() -> admin.actualizarTipoCambio(ADMIN, usd, "0", AHORA)));
        assertEquals("tipo-2", campo(() -> admin.actualizarTipoCambio(ADMIN, usd, "1,12345", AHORA)));
        // El EUR es la divisa pivote (C-06)
        assertEquals("tipo-1", campo(() -> admin.actualizarTipoCambio(ADMIN, divisas.porId.get(1), "2", AHORA)));
    }

    @Test
    void corregirPrecioYSimularUnDia() {
        Activo san = activos.porId.get(1);
        admin.actualizarPrecio(ADMIN, san, "4,25", AHORA);
        assertEquals(new BigDecimal("4.2500"), san.getPrecioActual());
        assertEquals("precio-1", campo(() -> admin.actualizarPrecio(ADMIN, san, "-1", AHORA)));

        admin.simularDiaDeMercado(ADMIN, AHORA);
        assertNotEquals(new BigDecimal("4.2500"), san.getPrecioActual());
    }

    @Test
    void crearEditarYEliminarBroker() {
        Broker b = admin.guardarBroker(ADMIN, 0, " Nuevo Broker ", "desc", "1 € por orden", "1,5", ACCIONES,
                Nivel.PRINCIPIANTE, "CNMV", "https://nuevo.example.com");
        assertEquals(1, b.getIdBroker());
        assertEquals("Nuevo Broker", brokers.todos.get(0).getNombre());
        assertEquals(new BigDecimal("1.50"), b.getComisionMinima());

        admin.guardarBroker(ADMIN, 1, "Renombrado", null, null, "", ACCIONES, Nivel.AVANZADO, null,
                "https://nuevo.example.com");
        assertEquals("Renombrado", brokers.todos.get(0).getNombre());
        assertEquals(0, BigDecimal.ZERO.compareTo(brokers.todos.get(0).getComisionMinima())); // vacío = sin mínimo

        admin.eliminarBroker(ADMIN, 1);
        assertTrue(brokers.todos.isEmpty());
    }

    @Test
    void validacionesDelBroker() {
        assertEquals("nombre", campo(() -> admin.guardarBroker(ADMIN, 0, " ", null, null, "1", ACCIONES,
                Nivel.PRINCIPIANTE, null, "https://a.com")));
        assertEquals("url", campo(() -> admin.guardarBroker(ADMIN, 0, "X", null, null, "1", ACCIONES,
                Nivel.PRINCIPIANTE, null, "javascript:alert(1)")));
        assertEquals("tipos", campo(() -> admin.guardarBroker(ADMIN, 0, "X", null, null, "1", Set.of(),
                Nivel.PRINCIPIANTE, null, "https://a.com")));
        assertEquals("comision", campo(() -> admin.guardarBroker(ADMIN, 0, "X", null, null, "-2", ACCIONES,
                Nivel.PRINCIPIANTE, null, "https://a.com")));
        assertTrue(brokers.todos.isEmpty());
    }

    @Test
    void leccionesConSusPreguntas() {
        Leccion l = admin.guardarLeccion(ADMIN, 0, "Ahorro", Nivel.PRINCIPIANTE, "Contenido");
        Pregunta p = admin.guardarPregunta(ADMIN, 0, l.getIdLeccion(), "¿Qué es ahorrar?", "a", "b", "c", "d", 'b');
        assertEquals('B', p.getRespuestaCorrecta());
        assertEquals(1, admin.preguntas(ADMIN, l.getIdLeccion()).size());

        admin.guardarPregunta(ADMIN, p.getIdPregunta(), l.getIdLeccion(), "¿Editada?", "a", "b", "c", "d", 'D');
        assertEquals("¿Editada?", preguntas.todas.get(0).getEnunciado());

        assertEquals("correcta", campo(() -> admin.guardarPregunta(ADMIN, 0, l.getIdLeccion(), "¿?", "a", "b", "c",
                "d", 'E')));
        assertEquals("opcionC", campo(() -> admin.guardarPregunta(ADMIN, 0, l.getIdLeccion(), "¿?", "a", "b", " ",
                "d", 'A')));
        assertEquals("titulo", campo(() -> admin.guardarLeccion(ADMIN, 0, "", Nivel.PRINCIPIANTE, "x")));
        assertEquals("nivel", campo(() -> admin.guardarLeccion(ADMIN, 0, "Título", null, "x")));

        admin.eliminarPregunta(ADMIN, p.getIdPregunta());
        admin.eliminarLeccion(ADMIN, l.getIdLeccion());
        assertTrue(lecciones.todas.isEmpty());
        assertThrows(ValidacionException.class, () -> admin.eliminarLeccion(ADMIN, l.getIdLeccion()));
    }

    private static String campo(Runnable accion) {
        return assertThrows(ValidacionException.class, accion::run).getCampo();
    }
}
