package com.eduwealth.service;

import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.ProgresoLeccion;
import com.eduwealth.model.TipoOperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-08 con RN-05 (aprobado ≥ 70 %), RN-08 (desbloqueo por niveles), C-10 (mejor nota) y RN-07. */
class EducacionServiceTest {

    private static final int USUARIO = 1;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);

    private DaosEnMemoria.Progresos progresos;
    private DaosEnMemoria.Operaciones operaciones;
    private DaosEnMemoria.Preguntas preguntas;
    private EducacionService servicio;

    @BeforeEach
    void preparar() {
        DaosEnMemoria.Lecciones lecciones = new DaosEnMemoria.Lecciones()
                .con(new Leccion(1, "Mercados", "Texto de la lección", Nivel.PRINCIPIANTE))
                .con(new Leccion(2, "Interés compuesto", "Texto", Nivel.PRINCIPIANTE))
                .con(new Leccion(3, "Diversificación", "Texto", Nivel.INTERMEDIO))
                .con(new Leccion(4, "Análisis técnico", "Texto", Nivel.AVANZADO));
        preguntas = new DaosEnMemoria.Preguntas();
        int id = 1;
        for (int leccion = 1; leccion <= 4; leccion++) {
            int n = leccion == 1 ? 10 : 5;
            for (int i = 0; i < n; i++) {
                preguntas.con(new Pregunta(id++, leccion, "¿Pregunta?", "a", "b", "c", "d", 'A'));
            }
        }
        progresos = new DaosEnMemoria.Progresos();
        operaciones = new DaosEnMemoria.Operaciones();
        servicio = new EducacionService(lecciones, progresos, preguntas, operaciones, new DaosEnMemoria.Transacciones());
    }

    @Test
    void alEmpezarSoloEstaDisponibleElNivelPrincipiante() {
        List<LeccionRuta> ruta = servicio.rutaDeAprendizaje(USUARIO);

        assertEquals(Disponibilidad.DISPONIBLE, ruta.get(0).disponibilidad());
        assertEquals(Disponibilidad.DISPONIBLE, ruta.get(1).disponibilidad());
        assertEquals(Disponibilidad.BLOQUEADA, ruta.get(2).disponibilidad());
        assertEquals(Disponibilidad.BLOQUEADA, ruta.get(3).disponibilidad());
        assertEquals(10, ruta.get(0).preguntas());
        assertNull(ruta.get(0).mejorNota());
    }

    @Test
    void conUn70SeApruebaYConMenosNo() {
        // RN-05: 7 de 10 = 70 % → aprobado
        ResultadoTest aprobado = servicio.corregirTest(USUARIO, 1, respuestas(1, 7), AHORA);
        assertEquals(new BigDecimal("70.00"), aprobado.nota());
        assertTrue(aprobado.aprobado());
        assertEquals(7, aprobado.aciertos());
        assertEquals(10, aprobado.correcciones().size());

        ResultadoTest suspenso = servicio.corregirTest(USUARIO, 2, respuestas(2, 3), AHORA);
        assertEquals(new BigDecimal("60.00"), suspenso.nota());
        assertFalse(suspenso.aprobado());
        ProgresoLeccion p = progreso(2);
        assertFalse(p.isCompletada());
        assertNull(p.getFechaCompletado());
    }

    @Test
    void seGuardaLaMejorNotaYUnSuspensoNoQuitaElAprobado() {
        servicio.corregirTest(USUARIO, 1, respuestas(1, 8), AHORA);
        ResultadoTest peor = servicio.corregirTest(USUARIO, 1, respuestas(1, 5), AHORA.plusDays(1));

        assertFalse(peor.aprobado());
        assertFalse(peor.nuevaMejorNota());
        assertEquals(new BigDecimal("80.00"), peor.mejorNota());
        ProgresoLeccion p = progreso(1);
        assertTrue(p.isCompletada());
        assertEquals(new BigDecimal("80.00"), p.getNotaTest());
        assertEquals(AHORA, p.getFechaCompletado()); // fecha del primer aprobado
        assertEquals(1, progresos.todos.size());
    }

    @Test
    void aprobarTrasSuspenderMarcaLaLeccionYSuFecha() {
        servicio.corregirTest(USUARIO, 1, respuestas(1, 6), AHORA);
        ResultadoTest r = servicio.corregirTest(USUARIO, 1, respuestas(1, 9), AHORA.plusDays(2));

        assertTrue(r.nuevaMejorNota());
        ProgresoLeccion p = progreso(1);
        assertTrue(p.isCompletada());
        assertEquals(new BigDecimal("90.00"), p.getNotaTest());
        assertEquals(AHORA.plusDays(2), p.getFechaCompletado());
    }

    @Test
    void aprobarTodoElNivelDesbloqueaElSiguiente() {
        // RN-08
        assertNull(servicio.corregirTest(USUARIO, 1, respuestas(1, 10), AHORA).nivelDesbloqueado());
        ResultadoTest r = servicio.corregirTest(USUARIO, 2, respuestas(2, 5), AHORA);

        assertEquals(Nivel.INTERMEDIO, r.nivelDesbloqueado());
        List<LeccionRuta> ruta = servicio.rutaDeAprendizaje(USUARIO);
        assertEquals(Disponibilidad.SUPERADA, ruta.get(0).disponibilidad());
        assertEquals(Disponibilidad.DISPONIBLE, ruta.get(2).disponibilidad());
        assertEquals(Disponibilidad.BLOQUEADA, ruta.get(3).disponibilidad());
        // Repetir una lección ya aprobada no vuelve a «desbloquear» nada
        assertNull(servicio.corregirTest(USUARIO, 2, respuestas(2, 5), AHORA).nivelDesbloqueado());
    }

    @Test
    void noSePuedeHacerElTestDeUnaLeccionBloqueada() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.corregirTest(USUARIO, 3, respuestas(3, 5), AHORA));
        assertTrue(e.getMessage().contains("Principiante"));
        assertThrows(ValidacionException.class, () -> servicio.abrirLeccion(USUARIO, 4));
        assertTrue(progresos.todos.isEmpty());
    }

    @Test
    void hayQueResponderTodasLasPreguntas() {
        Map<Integer, Character> incompletas = respuestas(2, 5);
        incompletas.remove(incompletas.keySet().iterator().next());
        incompletas.put(incompletas.keySet().iterator().next(), 'X'); // opción no válida

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.corregirTest(USUARIO, 2, incompletas, AHORA));
        assertTrue(e.getMessage().contains("faltan 2"));
        assertTrue(progresos.todos.isEmpty());
    }

    @Test
    void resumenConMedidorDePreparacion() {
        servicio.corregirTest(USUARIO, 1, respuestas(1, 10), AHORA); // 100
        servicio.corregirTest(USUARIO, 2, respuestas(2, 4), AHORA);  // 80
        for (int i = 0; i < 5; i++) {
            operaciones.todas.add(new OperacionSimulada(i, USUARIO, 1, TipoOperacion.COMPRA,
                    BigDecimal.ONE, BigDecimal.TEN, null, AHORA));
        }

        ResumenEducacion r = servicio.resumen(USUARIO);

        assertEquals(2, r.aprobadas());
        assertEquals(4, r.totales());
        assertEquals(50, r.preparacion()); // 70 × 2/4 + 30 × 5/10
        assertFalse(r.listo());
        assertEquals(new BigDecimal("9.0"), r.notaMedia());
        assertEquals(3, r.siguienteLeccion().getIdLeccion());
        assertEquals(Nivel.INTERMEDIO, r.nivelActual());
    }

    @Test
    void tiempoEstimadoDeLectura() {
        Leccion corta = new Leccion(9, "x", "palabra ".repeat(300), Nivel.PRINCIPIANTE);
        assertEquals(2 + 5, EducacionService.minutosEstimados(corta, 5)); // 300 palabras a 150/min + 5 preguntas
    }

    /** Responde bien las {@code aciertos} primeras preguntas de la lección y mal el resto. */
    private Map<Integer, Character> respuestas(int idLeccion, int aciertos) {
        Map<Integer, Character> r = new HashMap<>();
        List<Pregunta> lista = preguntas.listarPorLeccion(idLeccion);
        for (int i = 0; i < lista.size(); i++) {
            r.put(lista.get(i).getIdPregunta(), i < aciertos ? 'A' : 'B');
        }
        return r;
    }

    private ProgresoLeccion progreso(int idLeccion) {
        return progresos.listarPorUsuario(USUARIO).stream()
                .filter(p -> p.getIdLeccion() == idLeccion).findFirst().orElseThrow();
    }
}
