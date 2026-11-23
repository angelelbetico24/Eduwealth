package com.eduwealth.service;

import com.eduwealth.model.EstadoPresupuesto;

import java.math.BigDecimal;

/**
 * Situación de una categoría en un mes: límite, gastado, lo que queda (negativo si se ha excedido),
 * porcentaje con 1 decimal y estado según RN-03.
 */
public record CategoriaPresupuesto(int idCategoria, String categoria, BigDecimal limite, BigDecimal gastado,
                                   BigDecimal restante, BigDecimal porcentaje, EstadoPresupuesto estado) {
}
