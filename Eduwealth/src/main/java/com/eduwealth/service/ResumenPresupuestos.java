package com.eduwealth.service;

import com.eduwealth.model.EstadoPresupuesto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Presupuestos de un mes con los totales de todas las categorías.
 * {@code estado} es null si el usuario no ha definido presupuestos ese mes.
 */
public record ResumenPresupuestos(YearMonth mes, BigDecimal limite, BigDecimal gastado, BigDecimal disponible,
                                  BigDecimal porcentaje, EstadoPresupuesto estado,
                                  List<CategoriaPresupuesto> categorias) {

    public boolean vacio() {
        return categorias.isEmpty();
    }
}
