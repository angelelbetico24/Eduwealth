package com.eduwealth.dao;

/**
 * Unidad de trabajo que agrupa varias operaciones de los DAO de forma atómica.
 * Si se cierra sin llamar a {@link #confirmar()}, se deshacen todos los cambios (rollback).
 * <p>
 * Los servicios solo conocen esta interfaz: no dependen de JDBC.
 * <pre>
 * try (Transaccion tx = gestor.iniciar()) {
 *     ...operaciones con tx...
 *     tx.confirmar();
 * }
 * </pre>
 */
public interface Transaccion extends AutoCloseable {

    void confirmar();

    /** Deshace los cambios si no se han confirmado y libera la conexión. */
    @Override
    void close();
}
