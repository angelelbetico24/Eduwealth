package com.eduwealth.service;

import com.eduwealth.model.Broker;
import com.eduwealth.model.Leccion;

/**
 * Datos ya calculados que muestra el Panel Principal: cartera (calculada por el simulador),
 * presupuestos del mes, progreso educativo y broker recomendado.
 */
public record ResumenPanel(
        ResumenCartera cartera,
        ResumenPresupuestos presupuestos,
        Educacion educacion,
        Broker brokerRecomendado) {

    /** Progreso educativo y medidor de preparación (RN-07). {@code siguienteLeccion} es null si las ha aprobado todas. */
    public record Educacion(int aprobadas, int totales, int preparacion, boolean listo, Leccion siguienteLeccion) {
    }
}
