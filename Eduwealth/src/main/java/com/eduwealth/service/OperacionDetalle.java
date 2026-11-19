package com.eduwealth.service;

import com.eduwealth.model.TipoOperacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del historial de operaciones (CU-07). Importes en la divisa del activo. */
public record OperacionDetalle(LocalDateTime fecha, TipoOperacion tipo, String ticker, String nombre,
                               BigDecimal cantidad, BigDecimal precio, BigDecimal importe,
                               BigDecimal pnlRealizado, String simbolo) {
}
