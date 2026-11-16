package com.eduwealth.dao;

import com.eduwealth.model.Activo;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code activos} (catálogo del simulador, CU-03). */
public interface ActivoDAO {

    List<Activo> listarTodos();

    Optional<Activo> buscarPorId(int idActivo);

    Optional<Activo> buscarPorTicker(String ticker);
}
