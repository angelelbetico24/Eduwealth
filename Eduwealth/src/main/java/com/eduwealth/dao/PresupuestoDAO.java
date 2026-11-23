package com.eduwealth.dao;

import com.eduwealth.model.Presupuesto;
import com.eduwealth.model.ResumenCategoria;

import java.util.List;

/** Acceso a datos de la tabla {@code presupuestos} (CU-10). */
public interface PresupuestoDAO {

    /** Presupuestos del usuario en el mes indicado con el total gastado en cada categoría. */
    List<ResumenCategoria> listarResumenMes(int idUsuario, int mes, int anio);

    /**
     * Crea el presupuesto o, si ya existe uno para el mismo usuario, categoría y mes,
     * actualiza su límite (se apoya en la restricción UNIQUE de C-03).
     */
    void guardar(Presupuesto presupuesto);

    /** Elimina el presupuesto de una categoría en un mes. Devuelve si existía. */
    boolean eliminar(int idUsuario, int idCategoria, int mes, int anio);
}
