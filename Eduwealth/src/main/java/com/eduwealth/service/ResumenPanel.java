package com.eduwealth.service;

import com.eduwealth.model.Broker;

/**
 * Datos ya calculados que muestra el Panel Principal: cartera (calculada por el simulador),
 * presupuestos del mes, progreso educativo y broker recomendado.
 */
public record ResumenPanel(
        ResumenCartera cartera,
        ResumenPresupuestos presupuestos,
        ResumenEducacion educacion,
        Broker brokerRecomendado) {

}
