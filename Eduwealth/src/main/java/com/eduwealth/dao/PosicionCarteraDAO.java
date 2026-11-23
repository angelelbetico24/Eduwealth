package com.eduwealth.dao;

import com.eduwealth.model.PosicionCartera;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code posiciones_cartera}. */
public interface PosicionCarteraDAO {

    List<PosicionCartera> listarPorUsuario(int idUsuario);

    /** Posición del usuario en un activo, bloqueada hasta el fin de la transacción. */
    Optional<PosicionCartera> bloquear(Transaccion tx, int idUsuario, int idActivo);

    void insertar(Transaccion tx, PosicionCartera posicion);

    void actualizar(Transaccion tx, PosicionCartera posicion);

    void eliminar(Transaccion tx, int idPosicion);

    /** Vacía la cartera del usuario (reinicio del simulador, RF-19). */
    void eliminarTodas(Transaccion tx, int idUsuario);
}
