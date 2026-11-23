package com.eduwealth.service;

import com.eduwealth.model.Divisa;
import com.eduwealth.model.EstadoPresupuesto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas de negocio puras: RN-03 (umbrales), RN-04 (conversión) y RN-07 (preparación). */
class ReglasNegocioTest {

    private final PresupuestoService presupuestos = new PresupuestoService(null, null, null, null);
    private final DivisaService divisas = new DivisaService(null);
    private final EducacionService educacion = new EducacionService(null, null);

    private static final Divisa EUR = new Divisa(1, "EUR", "Euro", "€", new BigDecimal("1.0000"), null);
    private static final Divisa USD = new Divisa(2, "USD", "Dólar", "$", new BigDecimal("1.0850"), null);
    private static final Divisa GBP = new Divisa(3, "GBP", "Libra", "£", new BigDecimal("0.8550"), null);

    // ---------- RN-03 ----------
    @Test
    void umbralesDePresupuestoSinHuecos() {
        BigDecimal limite = new BigDecimal("500");
        assertEquals(EstadoPresupuesto.DENTRO_LIMITE, presupuestos.evaluarUmbral(new BigDecimal("399.99"), limite));
        assertEquals(EstadoPresupuesto.EN_ALERTA, presupuestos.evaluarUmbral(new BigDecimal("400"), limite));
        // 99,9 %: sigue en alerta aunque al redondear a entero sería 100 % (C-16)
        assertEquals(EstadoPresupuesto.EN_ALERTA, presupuestos.evaluarUmbral(new BigDecimal("499.50"), limite));
        assertEquals(EstadoPresupuesto.EXCEDIDO, presupuestos.evaluarUmbral(new BigDecimal("500"), limite));
        assertEquals(EstadoPresupuesto.EXCEDIDO, presupuestos.evaluarUmbral(new BigDecimal("650"), limite));
    }

    @Test
    void porcentajeDeGastoConUnDecimal() {
        assertEquals(new BigDecimal("85.0"), presupuestos.porcentaje(new BigDecimal("425"), new BigDecimal("500")));
        assertEquals(new BigDecimal("47.5"), presupuestos.porcentaje(new BigDecimal("95"), new BigDecimal("200")));
    }

    // ---------- RN-04 ----------
    @Test
    void convierteUsandoElEuroComoPivote() {
        assertEquals(new BigDecimal("92.17"), redondear(divisas.convertir(new BigDecimal("100"), USD, EUR)));
        assertEquals(new BigDecimal("108.50"), redondear(divisas.convertir(new BigDecimal("100"), EUR, USD)));
        // USD → GBP pasando por EUR: 100 / 1,085 × 0,855
        assertEquals(new BigDecimal("78.80"), redondear(divisas.convertir(new BigDecimal("100"), USD, GBP)));
        assertEquals(new BigDecimal("100"), divisas.convertir(new BigDecimal("100"), EUR, EUR));
    }

    @Test
    void detectaTiposDeCambioDesactualizados() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 16, 12, 0);
        Divisa reciente = new Divisa(2, "USD", "Dólar", "$", BigDecimal.ONE, ahora.minusDays(2));
        Divisa antigua = new Divisa(2, "USD", "Dólar", "$", BigDecimal.ONE, ahora.minusDays(8));
        assertFalse(divisas.estaDesactualizada(reciente, ahora));
        assertTrue(divisas.estaDesactualizada(antigua, ahora));
    }

    // ---------- RN-07 ----------
    @Test
    void medidorDePreparacion() {
        assertEquals(0, educacion.calcularPreparacion(0, 6, 0));
        assertEquals(35, educacion.calcularPreparacion(3, 6, 0));
        assertEquals(30, educacion.calcularPreparacion(0, 6, 10));
        assertEquals(30, educacion.calcularPreparacion(0, 6, 50)); // la práctica aporta como máximo un 30 %
        assertEquals(100, educacion.calcularPreparacion(6, 6, 10));
        assertEquals(15, educacion.calcularPreparacion(0, 0, 5)); // sin lecciones no se divide por cero
    }

    @Test
    void listoParaInversionRealDesdeEl75() {
        assertFalse(educacion.estaListo(74));
        assertTrue(educacion.estaListo(75));
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
