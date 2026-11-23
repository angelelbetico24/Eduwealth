package com.eduwealth.service;

import com.eduwealth.model.Activo;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Gasto;
import com.eduwealth.model.Presupuesto;
import com.eduwealth.model.Rol;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF-03 (nombre, contraseña, divisa base con C-28) y RF-19 (reinicio del simulador). */
class PerfilServiceTest {

    private static final Divisa EUR = new Divisa(1, "EUR", "Euro", "€", new BigDecimal("1.0000"), null);
    private static final Divisa USD = new Divisa(2, "USD", "Dólar", "$", new BigDecimal("1.0850"), null);

    private DaosEnMemoria.Usuarios usuarios;
    private DaosEnMemoria.Posiciones posiciones;
    private DaosEnMemoria.Operaciones operaciones;
    private DaosEnMemoria.Gastos gastos;
    private DaosEnMemoria.Presupuestos presupuestos;
    private DaosEnMemoria.Transacciones transacciones;
    private PerfilService perfil;
    private SimuladorService simulador;
    private int id;

    @BeforeEach
    void preparar() {
        usuarios = new DaosEnMemoria.Usuarios();
        posiciones = new DaosEnMemoria.Posiciones();
        operaciones = new DaosEnMemoria.Operaciones();
        gastos = new DaosEnMemoria.Gastos();
        presupuestos = new DaosEnMemoria.Presupuestos(gastos, new DaosEnMemoria.Categorias()
                .con(new CategoriaGasto(4, "Ocio", null)));
        transacciones = new DaosEnMemoria.Transacciones();
        DivisaService divisas = new DivisaService(new DaosEnMemoria.Divisas().con(EUR).con(USD));
        perfil = new PerfilService(usuarios, posiciones, operaciones, presupuestos, gastos, divisas, transacciones);
        DaosEnMemoria.Activos activos = new DaosEnMemoria.Activos()
                .con(new Activo(1, "SAN", "Santander", TipoActivo.ACCION, new BigDecimal("4.0000"), 1));
        simulador = new SimuladorService(usuarios, activos, posiciones, operaciones, transacciones, divisas);
        id = usuarios.insertar(new Usuario(0, "Ana", "ana@correo.com", BCrypt.hashpw("antigua123", BCrypt.gensalt(4)),
                Rol.USUARIO, new BigDecimal("10000.00"), EUR.getIdDivisa(), null));
    }

    @Test
    void cambiarNombre() {
        assertEquals("Ana López", perfil.cambiarNombre(id, "  Ana López "));
        assertEquals("Ana López", usuarios.porId.get(id).getNombre());
        assertEquals("nombre", assertThrows(ValidacionException.class, () -> perfil.cambiarNombre(id, " ")).getCampo());
    }

    @Test
    void cambiarPasswordCompruebaLaActual() {
        assertEquals("actual", campoError("mala", "nueva1234", "nueva1234"));
        assertEquals("nueva", campoError("antigua123", "corta", "corta"));
        assertEquals("nueva", campoError("antigua123", "antigua123", "antigua123"));
        assertEquals("confirmacion", campoError("antigua123", "nueva1234", "otra12345"));

        perfil.cambiarPassword(id, "antigua123", "nueva1234", "nueva1234");
        assertTrue(BCrypt.checkpw("nueva1234", usuarios.porId.get(id).getPasswordHash()));
    }

    @Test
    void cambiarDivisaConvierteSaldoCapitalPresupuestosYGastos() {
        presupuestos.todos.add(new Presupuesto(1, id, 4, new BigDecimal("500.00"), 11, 2026));
        gastos.insertar(new Gasto(0, id, 4, new BigDecimal("100.00"), LocalDate.of(2026, 11, 20), null));

        Usuario u = perfil.cambiarDivisaBase(id, USD);

        assertEquals(USD.getIdDivisa(), u.getIdDivisaBase());
        assertEquals(new BigDecimal("10850.00"), u.getSaldoFicticio());
        assertEquals(new BigDecimal("10850.00"), u.getCapitalInicial());
        assertEquals(new BigDecimal("542.50"), presupuestos.todos.get(0).getMontoLimite());
        assertEquals(new BigDecimal("108.50"), gastos.todos.get(0).getMonto());
        assertEquals(1, transacciones.confirmadas);
    }

    @Test
    void laRentabilidadNoCambiaAlCambiarDeDivisa() {
        // Sin operaciones, la rentabilidad debe seguir siendo 0 en la nueva divisa (C-28)
        perfil.cambiarDivisaBase(id, USD);
        ResumenCartera r = simulador.resumenCartera(usuarios.porId.get(id), LocalDateTime.now());
        assertEquals(0, r.rentabilidad().signum());
        assertEquals(new BigDecimal("10850.00"), r.capitalInicial());
    }

    @Test
    void cambiarALaMismaDivisaNoHaceNada() {
        perfil.cambiarDivisaBase(id, EUR);
        assertEquals(new BigDecimal("10000.00"), usuarios.porId.get(id).getSaldoFicticio());
        assertEquals(0, transacciones.confirmadas);
    }

    @Test
    void reiniciarBorraCarteraEHistorialYRestableceElSaldo() {
        simulador.comprar(id, 1, new BigDecimal("100"));
        presupuestos.todos.add(new Presupuesto(1, id, 4, new BigDecimal("500.00"), 11, 2026));

        BigDecimal saldo = perfil.reiniciarSimulador(id);

        assertEquals(new BigDecimal("10000.00"), saldo);
        assertEquals(new BigDecimal("10000.00"), usuarios.porId.get(id).getSaldoFicticio());
        assertTrue(posiciones.porId.isEmpty());
        assertTrue(operaciones.todas.isEmpty());
        assertEquals(1, presupuestos.todos.size()); // los presupuestos no se tocan (C-14)
    }

    private String campoError(String actual, String nueva, String confirmacion) {
        return assertThrows(ValidacionException.class,
                () -> perfil.cambiarPassword(id, actual, nueva, confirmacion)).getCampo();
    }
}
