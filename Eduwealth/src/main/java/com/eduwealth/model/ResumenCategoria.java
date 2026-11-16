package com.eduwealth.model;

import java.math.BigDecimal;

/**
 * Presupuesto de una categoría en un mes junto con lo gastado en ella
 * (resultado de consulta, no es una tabla).
 */
public record ResumenCategoria(int idCategoria, String categoria, BigDecimal limite, BigDecimal gastado) {
}
