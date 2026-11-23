package com.eduwealth.dao;

import com.eduwealth.model.Pregunta;

import java.util.List;
import java.util.Map;

/** Acceso a datos de la tabla {@code preguntas} (test de cada lección, C-01). */
public interface PreguntaDAO {

    /** Preguntas de una lección en orden de creación. */
    List<Pregunta> listarPorLeccion(int idLeccion);

    /** Número de preguntas de cada lección (id_leccion → total), en una sola consulta. */
    Map<Integer, Integer> contarPorLeccion();
}
