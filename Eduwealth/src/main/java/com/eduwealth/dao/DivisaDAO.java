package com.eduwealth.dao;

import com.eduwealth.model.Divisa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code divisas}. */
public interface DivisaDAO {

    List<Divisa> listarTodas();

    Optional<Divisa> buscarPorId(int idDivisa);

    Optional<Divisa> buscarPorCodigo(String codigo);

    /** Nuevo tipo de cambio (unidades por 1 EUR, C-06) y fecha de actualización (C-08). */
    void actualizarTipoCambio(int idDivisa, BigDecimal tipoCambio, LocalDateTime fecha);
}
