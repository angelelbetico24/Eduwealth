package com.eduwealth.service;

import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Leccion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos ya calculados que muestra el Panel Principal. Los importes globales están en la
 * divisa base del usuario; los de cada fila de cartera, en la divisa del activo.
 */
public record ResumenPanel(
        Divisa divisaBase,
        BigDecimal saldoDisponible,
        BigDecimal valorCartera,
        BigDecimal patrimonioTotal,
        BigDecimal rentabilidad,
        BigDecimal rentabilidadPorcentaje,
        List<FilaCartera> cartera,
        Presupuestos presupuestos,
        Educacion educacion,
        Broker brokerRecomendado,
        boolean tiposCambioDesactualizados) {

    /** Una posición de la cartera lista para mostrar. */
    public record FilaCartera(String ticker, String nombre, BigDecimal cantidad, BigDecimal precioMedio,
                              BigDecimal precioActual, BigDecimal pnl, BigDecimal pnlPorcentaje,
                              String simbolo) {
    }

    /** Totales del mes y categorías que requieren atención (RN-03). {@code estado} es null sin presupuestos. */
    public record Presupuestos(BigDecimal limite, BigDecimal gastado, BigDecimal porcentaje,
                               EstadoPresupuesto estado, int categorias, List<Aviso> avisos) {
    }

    public record Aviso(String categoria, BigDecimal gastado, BigDecimal limite, BigDecimal porcentaje,
                        EstadoPresupuesto estado) {
    }

    /** Progreso educativo y medidor de preparación (RN-07). {@code siguienteLeccion} es null si las ha aprobado todas. */
    public record Educacion(int aprobadas, int totales, int preparacion, boolean listo, Leccion siguienteLeccion) {
    }
}
