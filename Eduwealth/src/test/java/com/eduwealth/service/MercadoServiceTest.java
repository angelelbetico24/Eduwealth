package com.eduwealth.service;

import com.eduwealth.model.Activo;
import com.eduwealth.model.TipoActivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C-26: movimiento simulado del mercado por días transcurridos. */
class MercadoServiceTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 19, 10, 0);
    private static final BigDecimal PRECIO_INICIAL = new BigDecimal("100.0000");

    private DaosEnMemoria.Activos activos;
    private DaosEnMemoria.Transacciones transacciones;
    private MercadoService mercado;

    @BeforeEach
    void preparar() {
        activos = new DaosEnMemoria.Activos()
                .con(new Activo(1, "AAPL", "Apple", TipoActivo.ACCION, PRECIO_INICIAL, 2, HOY.minusDays(3)))
                .con(new Activo(2, "BTC", "Bitcoin", TipoActivo.CRIPTO, PRECIO_INICIAL, 2, HOY.minusHours(2)));
        transacciones = new DaosEnMemoria.Transacciones();
        mercado = new MercadoService(activos, transacciones, new Random(42)); // semilla fija: resultado reproducible
    }

    @Test
    void soloCambianLosActivosConDiasPendientes() {
        int cambiados = mercado.actualizarPrecios(HOY);

        assertEquals(1, cambiados);
        assertNotEquals(0, PRECIO_INICIAL.compareTo(activos.porId.get(1).getPrecioActual()));
        assertEquals(HOY, activos.porId.get(1).getFechaActualizacion());
        // BTC ya se actualizó hoy (hace 2 horas): no se toca
        assertEquals(0, PRECIO_INICIAL.compareTo(activos.porId.get(2).getPrecioActual()));
        assertEquals(1, transacciones.confirmadas);
    }

    @Test
    void esIdempotenteDentroDelMismoDia() {
        mercado.actualizarPrecios(HOY);
        BigDecimal tras1 = activos.porId.get(1).getPrecioActual();

        assertEquals(0, mercado.actualizarPrecios(HOY.plusHours(5)));
        assertEquals(tras1, activos.porId.get(1).getPrecioActual());
    }

    @Test
    void cuentaDiasNaturalesConTope() {
        Activo a = activos.porId.get(1);
        a.setFechaActualizacion(LocalDateTime.of(2026, 11, 18, 23, 59));
        assertEquals(1, mercado.diasPendientes(a, LocalDateTime.of(2026, 11, 19, 0, 1)));
        a.setFechaActualizacion(HOY.minusYears(1));
        assertEquals(MercadoService.MAX_DIAS_POR_ACTUALIZACION, mercado.diasPendientes(a, HOY));
        a.setFechaActualizacion(HOY.plusDays(1)); // fecha futura (reloj cambiado): no retrocede
        assertEquals(0, mercado.diasPendientes(a, HOY));
    }

    @Test
    void elPrecioSiempreEsPositivoYConCuatroDecimales() {
        BigDecimal precio = new BigDecimal("0.0002");
        for (int i = 0; i < 200; i++) {
            precio = mercado.simularDias(precio, TipoActivo.CRIPTO, 30);
            assertTrue(precio.signum() > 0);
            assertEquals(4, precio.scale());
        }
    }

    @Test
    void lasCriptomonedasSonMasVolatilesQueLosEtf() {
        double cripto = desviacionDiaria(TipoActivo.CRIPTO);
        double etf = desviacionDiaria(TipoActivo.ETF);
        assertTrue(cripto > 3 * etf, "cripto=" + cripto + " etf=" + etf);
    }

    private double desviacionDiaria(TipoActivo tipo) {
        int n = 2000;
        double suma = 0;
        double sumaCuadrados = 0;
        for (int i = 0; i < n; i++) {
            double r = mercado.simularDias(PRECIO_INICIAL, tipo, 1).doubleValue() / 100.0 - 1;
            suma += r;
            sumaCuadrados += r * r;
        }
        double media = suma / n;
        return Math.sqrt(sumaCuadrados / n - media * media);
    }
}
