package com.eduwealth.service;

import com.eduwealth.model.OperacionSimulada;

import java.math.BigDecimal;

/** Resultado de una compra o venta ya guardada. */
public record ResultadoOperacion(OperacionSimulada operacion, BigDecimal importeBase, BigDecimal saldoRestante) {
}
