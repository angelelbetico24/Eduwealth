package com.eduwealth.dao;

import com.eduwealth.model.OperacionSimulada;

import java.util.List;

/** Acceso a datos de la tabla {@code operaciones_simuladas}. */
public interface OperacionSimuladaDAO {

    int contarPorUsuario(int idUsuario);

    /** Historial del usuario, de la operación más reciente a la más antigua (CU-07). */
    List<OperacionSimulada> listarPorUsuario(int idUsuario);

    void insertar(Transaccion tx, OperacionSimulada operacion);
}
