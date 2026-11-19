package com.eduwealth.service;

import com.eduwealth.model.Divisa;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado de la cartera simulada (CU-06). Los totales están en la divisa base del usuario (RN-04);
 * cada posición, en la divisa de su activo.
 */
public record ResumenCartera(Divisa divisaBase, BigDecimal saldoDisponible, BigDecimal valorCartera,
                             BigDecimal pnlNoRealizado, BigDecimal patrimonioTotal, BigDecimal rentabilidad,
                             BigDecimal rentabilidadPorcentaje, List<PosicionDetalle> posiciones,
                             boolean tiposCambioDesactualizados) {
}
