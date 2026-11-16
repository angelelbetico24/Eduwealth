package com.eduwealth.dao;

import com.eduwealth.model.ProgresoLeccion;

import java.util.List;

/** Acceso a datos de la tabla {@code progresos_lecciones}. */
public interface ProgresoLeccionDAO {

    List<ProgresoLeccion> listarPorUsuario(int idUsuario);
}
