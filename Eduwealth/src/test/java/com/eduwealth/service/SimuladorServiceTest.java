package com.eduwealth.service;

import com.eduwealth.model.Activo;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.model.Rol;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.TipoOperacion;
import com.eduwealth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-04 / CU-05 con RN-01, RN-02 y RN-04, usando DAO en memoria. */
class SimuladorServiceTest {

    private static final Divisa EUR = new Divisa(1, "EUR", "Euro", "€", new BigDecimal("1.0000"), null);
    private static final Divisa USD = new Divisa(2, "USD", "Dólar", "$", new BigDecimal("1.0850"), null);
    private static final int AAPL = 1;
    private static final int SAN = 2;

    private DaosEnMemoria.Usuarios usuarios;
    private DaosEnMemoria.Activos activos;
    private DaosEnMemoria.Posiciones posiciones;
    private DaosEnMemoria.Operaciones operaciones;
    private DaosEnMemoria.Transacciones transacciones;
    private SimuladorService simulador;
    private int idUsuario;

    @BeforeEach
    void preparar() {
        usuarios = new DaosEnMemoria.Usuarios();
        activos = new DaosEnMemoria.Activos()
                .con(new Activo(AAPL, "AAPL", "Apple Inc.", TipoActivo.ACCION, new BigDecimal("195.0000"), 2))
                .con(new Activo(SAN, "SAN", "Banco Santander", TipoActivo.ACCION, new BigDecimal("3.9800"), 1));
        posiciones = new DaosEnMemoria.Posiciones();
        operaciones = new DaosEnMemoria.Operaciones();
        transacciones = new DaosEnMemoria.Transacciones();
        DivisaService divisas = new DivisaService(new DaosEnMemoria.Divisas().con(EUR).con(USD));
        simulador = new SimuladorService(usuarios, activos, posiciones, operaciones, transacciones, divisas);
        idUsuario = usuarios.insertar(new Usuario(0, "Ana", "ana@correo.com", "hash", Rol.USUARIO,
                new BigDecimal("10000.00"), EUR.getIdDivisa(), null));
    }

    @Test
    void comprarConvierteElCosteDescuentaSaldoYCreaLaPosicion() {
        ResultadoOperacion r = simulador.comprar(idUsuario, AAPL, new BigDecimal("10"));

        // 10 × 195 $ = 1.950 $ → 1.950 / 1,085 = 1.797,24 € (RN-04)
        assertEquals(new BigDecimal("1797.24"), r.importeBase());
        assertEquals(new BigDecimal("8202.76"), saldo());
        PosicionCartera p = posicion(AAPL);
        assertEquals(0, new BigDecimal("10").compareTo(p.getCantidadTitulos()));
        assertEquals(0, new BigDecimal("195").compareTo(p.getPrecioMedioCompra()));
        OperacionSimulada op = operaciones.todas.get(0);
        assertEquals(TipoOperacion.COMPRA, op.getTipoOperacion());
        assertNull(op.getPnlRealizado());
        assertEquals(1, transacciones.confirmadas);
    }

    @Test
    void comprarSinSaldoSuficienteNoModificaNada() {
        // RN-01: 1.000 × 195 $ ≈ 179.724 € > 10.000 €
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> simulador.comprar(idUsuario, AAPL, new BigDecimal("1000")));

        assertEquals("cantidad", e.getCampo());
        assertEquals(new BigDecimal("10000.00"), saldo());
        assertTrue(posiciones.porId.isEmpty());
        assertTrue(operaciones.todas.isEmpty());
        assertEquals(0, transacciones.confirmadas);
        assertEquals(1, transacciones.deshechas);
    }

    @Test
    void segundaCompraRecalculaElPrecioMedio() {
        simulador.comprar(idUsuario, SAN, new BigDecimal("100"));
        activos.porId.get(SAN).setPrecioActual(new BigDecimal("4.9800"));
        simulador.comprar(idUsuario, SAN, new BigDecimal("100"));

        PosicionCartera p = posicion(SAN);
        assertEquals(0, new BigDecimal("200").compareTo(p.getCantidadTitulos()));
        assertEquals(new BigDecimal("4.4800"), p.getPrecioMedioCompra()); // (398 + 498) / 200
        assertEquals(1, posiciones.porId.size());
    }

    @Test
    void ventaParcialRegistraPlusvaliaYSumaElIngreso() {
        simulador.comprar(idUsuario, SAN, new BigDecimal("100")); // 398,00 €
        activos.porId.get(SAN).setPrecioActual(new BigDecimal("5.0000"));

        ResultadoOperacion r = simulador.vender(idUsuario, SAN, new BigDecimal("40"));

        assertEquals(new BigDecimal("200.00"), r.importeBase());
        assertEquals(new BigDecimal("40.8000"), r.operacion().getPnlRealizado()); // (5 − 3,98) × 40
        assertEquals(new BigDecimal("9802.00"), saldo()); // 10.000 − 398 + 200
        PosicionCartera p = posicion(SAN);
        assertEquals(0, new BigDecimal("60").compareTo(p.getCantidadTitulos()));
        assertEquals(new BigDecimal("3.9800"), p.getPrecioMedioCompra()); // vender no cambia el PMC
    }

    @Test
    void venderTodosLosTitulosEliminaLaPosicion() {
        simulador.comprar(idUsuario, SAN, new BigDecimal("100"));
        simulador.vender(idUsuario, SAN, new BigDecimal("100"));

        assertTrue(posiciones.porId.isEmpty());
        assertEquals(2, operaciones.todas.size());
    }

    @Test
    void noSePuedenVenderMasTitulosDeLosPoseidos() {
        simulador.comprar(idUsuario, SAN, new BigDecimal("20"));

        // RN-02 (caso límite 6.9.3)
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> simulador.vender(idUsuario, SAN, new BigDecimal("50")));
        assertTrue(e.getMessage().contains("Solo tienes 20"));
        assertEquals(0, new BigDecimal("20").compareTo(posicion(SAN).getCantidadTitulos()));
        assertEquals(1, transacciones.deshechas);
    }

    @Test
    void noSePuedeVenderUnActivoQueNoSeTiene() {
        assertThrows(ValidacionException.class, () -> simulador.vender(idUsuario, AAPL, BigDecimal.ONE));
        assertEquals(new BigDecimal("10000.00"), saldo());
    }

    @Test
    void validaLaCantidadIntroducida() {
        assertEquals(0, new BigDecimal("10").compareTo(simulador.validarCantidad("10")));
        assertEquals(0, new BigDecimal("0.5").compareTo(simulador.validarCantidad(" 0,5 ")));
        assertThrows(ValidacionException.class, () -> simulador.validarCantidad(""));
        assertThrows(ValidacionException.class, () -> simulador.validarCantidad("abc"));
        assertThrows(ValidacionException.class, () -> simulador.validarCantidad("0"));
        assertThrows(ValidacionException.class, () -> simulador.validarCantidad("-3"));
        assertThrows(ValidacionException.class, () -> simulador.validarCantidad("1,12345"));
    }

    @Test
    void laCotizacionAvisaAntesDeConfirmar() {
        Activo aapl = activos.porId.get(AAPL);
        Cotizacion compraCara = simulador.cotizar(TipoOperacion.COMPRA, aapl, USD, EUR,
                new BigDecimal("100"), new BigDecimal("10000.00"), BigDecimal.ZERO);
        assertFalse(compraCara.permitida());
        assertTrue(compraCara.motivo().startsWith("Saldo virtual insuficiente"));

        Cotizacion venta = simulador.cotizar(TipoOperacion.VENTA, aapl, USD, EUR,
                new BigDecimal("5"), new BigDecimal("10000.00"), new BigDecimal("5"));
        assertTrue(venta.permitida());
        assertEquals(new BigDecimal("898.62"), venta.importeBase()); // 975 $ / 1,085
    }

    @Test
    void filtraElCatalogoPorTextoTipoYDivisa() {
        var todos = activos.listarTodos();
        assertEquals(1, simulador.filtrar(todos, "apple", null, null).size());
        assertEquals(1, simulador.filtrar(todos, "san", TipoActivo.ACCION, EUR).size());
        assertEquals(0, simulador.filtrar(todos, "", TipoActivo.CRIPTO, null).size());
        assertEquals(2, simulador.filtrar(todos, "  ", null, null).size());
    }

    @Test
    void sePuedeVenderUnRestoDeValorInferiorAUnCentimo() {
        // Revisión: 0,0004 SAN × 3,98 € = 0,0016 € → 0,00 €; vender todo no debe quedar bloqueado (C-24)
        simulador.comprar(idUsuario, SAN, new BigDecimal("1"));
        simulador.vender(idUsuario, SAN, new BigDecimal("0.9996"));

        Activo san = activos.porId.get(SAN);
        assertTrue(simulador.cotizar(TipoOperacion.VENTA, san, EUR, EUR, new BigDecimal("0.0004"),
                BigDecimal.ZERO, new BigDecimal("0.0004")).permitida());
        simulador.vender(idUsuario, SAN, new BigDecimal("0.0004"));
        assertTrue(posiciones.porId.isEmpty());

        // Una compra de importe ínfimo sigue rechazándose
        assertThrows(ValidacionException.class, () -> simulador.comprar(idUsuario, SAN, new BigDecimal("0.0001")));
    }

    @Test
    void milTitulosEscritosConPuntoDeMiles() {
        assertEquals(0, new BigDecimal("1000").compareTo(simulador.validarCantidad("1.000")));
    }

    @Test
    void precioMedioPonderado() {
        assertEquals(new BigDecimal("150.0000"), SimuladorService.calcularPrecioMedio(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("200")));
    }

    private BigDecimal saldo() {
        return usuarios.porId.get(idUsuario).getSaldoFicticio();
    }

    private PosicionCartera posicion(int idActivo) {
        return posiciones.listarPorUsuario(idUsuario).stream()
                .filter(p -> p.getIdActivo() == idActivo).findFirst().orElseThrow();
    }
}
