package com.eduwealth.dao;

import com.eduwealth.model.ResumenCategoria;

import java.util.List;

/** Acceso a datos de las tablas {@code presupuestos} y {@code gastos}. */
public interface PresupuestoDAO {

    /** Presupuestos del usuario en el mes indicado con el total gastado en cada categoría. */
    List<ResumenCategoria> listarResumenMes(int idUsuario, int mes, int anio);
}
