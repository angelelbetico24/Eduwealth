package com.eduwealth.dao;

import com.eduwealth.model.Leccion;

import java.util.List;

/** Acceso a datos de la tabla {@code lecciones}. */
public interface LeccionDAO {

    /** Lecciones ordenadas por nivel (principiante → avanzado) y por identificador. */
    List<Leccion> listarTodas();

    /** Inserta la lección y devuelve el identificador generado. */
    int insertar(Leccion leccion);

    void actualizar(Leccion leccion);

    /** Elimina la lección; sus preguntas y el progreso de los usuarios se borran en cascada (FK). */
    boolean eliminar(int idLeccion);
}
