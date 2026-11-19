package com.eduwealth.service;

import com.eduwealth.model.Activo;
import com.eduwealth.model.Divisa;

import java.math.BigDecimal;

/**
 * Posición de la cartera con su activo, valor de mercado y ganancia o pérdida no realizada.
 * Los importes están en la divisa del activo.
 */
public record PosicionDetalle(Activo activo, Divisa divisa, BigDecimal cantidad, BigDecimal precioMedio,
                              BigDecimal valorActual, BigDecimal pnl, BigDecimal pnlPorcentaje) {
}
