package com.eduwealth.dao;

import com.eduwealth.model.Leccion;

import java.util.List;

/** Acceso a datos de la tabla {@code lecciones}. */
public interface LeccionDAO {

    /** Lecciones ordenadas por nivel (principiante → avanzado) y por identificador. */
    List<Leccion> listarTodas();
}
