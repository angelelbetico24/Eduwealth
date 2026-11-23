package com.eduwealth.service;

import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.model.EstadoPresupuesto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-10 (definir presupuesto) y CU-11 (registrar gasto y evaluar umbral, RN-03) con DAO en memoria. */
class PresupuestoServiceTest {

    private static final int USUARIO = 1;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);
    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);
    private static final CategoriaGasto OCIO = new CategoriaGasto(4, "Ocio y Restauración", null);
    private static final CategoriaGasto TRANSPORTE = new CategoriaGasto(3, "Transporte", null);

    private DaosEnMemoria.Gastos gastos;
    private DaosEnMemoria.Presupuestos presupuestos;
    private DaosEnMemoria.Transacciones transacciones;
    private PresupuestoService servicio;

    @BeforeEach
    void preparar() {
        DaosEnMemoria.Categorias categorias = new DaosEnMemoria.Categorias().con(OCIO).con(TRANSPORTE);
        gastos = new DaosEnMemoria.Gastos();
        presupuestos = new DaosEnMemoria.Presupuestos(gastos, categorias);
        transacciones = new DaosEnMemoria.Transacciones();
        servicio = new PresupuestoService(presupuestos, gastos, categorias, transacciones);
        servicio.definirPresupuesto(USUARIO, OCIO, "500", NOVIEMBRE);
    }

    @Test
    void definirDosVecesElMismoMesAjustaElLimiteSinDuplicar() {
        servicio.definirPresupuesto(USUARIO, OCIO, "650,50", NOVIEMBRE);

        assertEquals(1, presupuestos.todos.size());
        assertEquals(new BigDecimal("650.50"), presupuestos.todos.get(0).getMontoLimite());
    }

    @Test
    void definirPresupuestoValidaLosDatos() {
        assertEquals("categoria", assertThrows(ValidacionException.class,
                () -> servicio.definirPresupuesto(USUARIO, null, "100", NOVIEMBRE)).getCampo());
        assertEquals("limite", assertThrows(ValidacionException.class,
                () -> servicio.definirPresupuesto(USUARIO, OCIO, "-100", NOVIEMBRE)).getCampo());
        assertEquals("mes", assertThrows(ValidacionException.class,
                () -> servicio.definirPresupuesto(USUARIO, OCIO, "100", null)).getCampo());
    }

    @Test
    void registrarGastoDevuelveElEstadoYAvisaAlCruzarUmbrales() {
        ResultadoGasto primero = servicio.registrarGasto(USUARIO, OCIO, "300", HOY, "Cena", HOY);
        assertEquals(EstadoPresupuesto.DENTRO_LIMITE, primero.categoria().estado());
        assertFalse(primero.cambioDeEstado());

        // 300 + 125 = 425 de 500 → 85 % → En Alerta (FA-01)
        ResultadoGasto segundo = servicio.registrarGasto(USUARIO, OCIO, "125", HOY, null, HOY);
        assertEquals(EstadoPresupuesto.EN_ALERTA, segundo.categoria().estado());
        assertEquals(new BigDecimal("85.0"), segundo.categoria().porcentaje());
        assertTrue(segundo.cambioDeEstado());

        // 425 + 75 = 500 → 100 % → Presupuesto Excedido (FA-02)
        ResultadoGasto tercero = servicio.registrarGasto(USUARIO, OCIO, "75", HOY, null, HOY);
        assertEquals(EstadoPresupuesto.EXCEDIDO, tercero.categoria().estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(tercero.categoria().restante()));
        assertEquals(3, gastos.todos.size());
    }

    @Test
    void noSeAnotanGastosEnCategoriasSinPresupuesto() {
        // Precondición de CU-11 según C-09
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrarGasto(USUARIO, TRANSPORTE, "20", HOY, null, HOY));
        assertEquals("categoria", e.getCampo());
        assertTrue(e.getMessage().contains("noviembre de 2026"));
        assertTrue(gastos.todos.isEmpty());
    }

    @Test
    void elGastoCuentaEnElMesDeSuFecha() {
        // Un gasto de octubre necesita presupuesto de octubre, aunque haya uno en noviembre
        assertThrows(ValidacionException.class,
                () -> servicio.registrarGasto(USUARIO, OCIO, "20", HOY.minusMonths(1), null, HOY));
    }

    @Test
    void validaImporteFechaYConcepto() {
        assertEquals("importe", campoConError("-5", HOY, null));    // E-01: negativo
        assertEquals("importe", campoConError("0", HOY, null));
        assertEquals("importe", campoConError("doce", HOY, null));  // E-01: formato no válido
        assertEquals("importe", campoConError("1,999", HOY, null)); // más de 2 decimales
        assertEquals("fecha", campoConError("10", null, null));
        assertEquals("fecha", campoConError("10", HOY.plusDays(1), null));
        assertEquals("concepto", campoConError("10", HOY, "x".repeat(151)));
        assertTrue(gastos.todos.isEmpty());
    }

    @Test
    void milEurosEscritosConPuntoDeMiles() {
        // Revisión: antes «1.000» se guardaba como 1,00 €
        assertEquals(new BigDecimal("1000.00"), servicio.validarImporte("1.000", "importe"));
        assertEquals(new BigDecimal("1234.50"), servicio.validarImporte("1.234,50", "importe"));
    }

    @Test
    void elConceptoVacioSeGuardaComoNulo() {
        servicio.registrarGasto(USUARIO, OCIO, " 12,5 ", HOY, "   ", HOY);
        assertNull(gastos.todos.get(0).getConcepto());
        assertEquals(new BigDecimal("12.50"), gastos.todos.get(0).getMonto());
    }

    @Test
    void resumenDelMesSumaTodasLasCategorias() {
        servicio.definirPresupuesto(USUARIO, TRANSPORTE, "200", NOVIEMBRE);
        servicio.registrarGasto(USUARIO, OCIO, "425", HOY, null, HOY);
        servicio.registrarGasto(USUARIO, TRANSPORTE, "95", HOY, null, HOY);

        ResumenPresupuestos r = servicio.resumenDelMes(USUARIO, NOVIEMBRE);
        assertEquals(0, new BigDecimal("700").compareTo(r.limite()));
        assertEquals(0, new BigDecimal("520").compareTo(r.gastado()));
        assertEquals(0, new BigDecimal("180").compareTo(r.disponible()));
        assertEquals(EstadoPresupuesto.DENTRO_LIMITE, r.estado()); // 74,3 %
        assertEquals(2, r.categorias().size());
    }

    @Test
    void unMesSinPresupuestosNoTieneEstado() {
        ResumenPresupuestos r = servicio.resumenDelMes(USUARIO, NOVIEMBRE.plusMonths(1));
        assertTrue(r.vacio());
        assertNull(r.estado());
    }

    @Test
    void eliminarPresupuestoYGasto() {
        ResultadoGasto g = servicio.registrarGasto(USUARIO, OCIO, "10", HOY, null, HOY);
        servicio.eliminarGasto(USUARIO, g.gasto().getIdGasto());
        assertTrue(gastos.todos.isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.eliminarGasto(2, 99)); // no existe o no es suyo

        servicio.eliminarPresupuesto(USUARIO, OCIO.getIdCategoria(), NOVIEMBRE);
        assertTrue(presupuestos.todos.isEmpty());
    }

    @Test
    void guardarLimitesAplicaTodosLosCambiosDelMes() {
        CategoriaGasto vivienda = new CategoriaGasto(2, "Vivienda", null);
        Map<CategoriaGasto, String> limites = new LinkedHashMap<>();
        limites.put(OCIO, "");             // sin gastos: se elimina
        limites.put(TRANSPORTE, "200,00"); // nuevo
        limites.put(vivienda, "  ");       // no tenía presupuesto: no hace nada

        int antes = transacciones.confirmadas;
        servicio.guardarLimites(USUARIO, NOVIEMBRE, limites);

        ResumenPresupuestos r = servicio.resumenDelMes(USUARIO, NOVIEMBRE);
        assertEquals(1, r.categorias().size());
        assertEquals("Transporte", r.categorias().get(0).categoria());
        assertEquals(antes + 1, transacciones.confirmadas); // todos los cambios en una sola transacción
    }

    @Test
    void noSeQuitaElLimiteDeUnaCategoriaConGastosYNoSeAplicaNada() {
        servicio.registrarGasto(USUARIO, OCIO, "95", HOY, null, HOY);
        Map<CategoriaGasto, String> limites = new LinkedHashMap<>();
        limites.put(TRANSPORTE, "200");
        limites.put(OCIO, "");

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.guardarLimites(USUARIO, NOVIEMBRE, limites));

        assertEquals("limite-" + OCIO.getIdCategoria(), e.getCampo());
        assertTrue(e.getMessage().contains("ya tiene gastos"));
        // Validación previa: tampoco se ha guardado Transporte
        assertEquals(1, servicio.resumenDelMes(USUARIO, NOVIEMBRE).categorias().size());
        assertThrows(ValidacionException.class,
                () -> servicio.eliminarPresupuesto(USUARIO, OCIO.getIdCategoria(), NOVIEMBRE));
    }

    @Test
    void unLimiteNoValidoIndicaSuCategoria() {
        Map<CategoriaGasto, String> limites = new LinkedHashMap<>();
        limites.put(TRANSPORTE, "-20");
        assertEquals("limite-" + TRANSPORTE.getIdCategoria(), assertThrows(ValidacionException.class,
                () -> servicio.guardarLimites(USUARIO, NOVIEMBRE, limites)).getCampo());
    }

    private String campoConError(String importe, LocalDate fecha, String concepto) {
        return assertThrows(ValidacionException.class,
                () -> servicio.registrarGasto(USUARIO, OCIO, importe, fecha, concepto, HOY)).getCampo();
    }
}
