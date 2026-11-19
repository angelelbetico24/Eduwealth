package com.eduwealth.dao;

import com.eduwealth.model.Activo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code activos} (catálogo del simulador, CU-03). */
public interface ActivoDAO {

    List<Activo> listarTodos();

    Optional<Activo> buscarPorId(int idActivo);

    Optional<Activo> buscarPorTicker(String ticker);

    /** Todos los activos, bloqueados hasta el fin de la transacción (evita aplicar dos veces el movimiento del mercado). */
    List<Activo> bloquearTodos(Transaccion tx);

    void actualizarPrecio(Transaccion tx, int idActivo, BigDecimal precio, LocalDateTime fecha);
}
