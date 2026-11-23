package com.eduwealth.service;

import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Gasto;

/**
 * Resultado de registrar un gasto (CU-11): el gasto guardado y cómo queda su categoría.
 * {@code estadoAnterior} permite avisar cuando el gasto hace cruzar un umbral (FA-01, FA-02).
 */
public record ResultadoGasto(Gasto gasto, CategoriaPresupuesto categoria, EstadoPresupuesto estadoAnterior) {

    public boolean cambioDeEstado() {
        return categoria.estado() != estadoAnterior;
    }
}
