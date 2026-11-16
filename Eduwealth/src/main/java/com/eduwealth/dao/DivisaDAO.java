package com.eduwealth.dao;

import com.eduwealth.model.Divisa;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code divisas}. */
public interface DivisaDAO {

    List<Divisa> listarTodas();

    Optional<Divisa> buscarPorId(int idDivisa);

    Optional<Divisa> buscarPorCodigo(String codigo);
}
