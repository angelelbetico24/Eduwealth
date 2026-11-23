package com.eduwealth.dao;

import com.eduwealth.model.ProgresoLeccion;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla {@code progresos_lecciones}. */
public interface ProgresoLeccionDAO {

    List<ProgresoLeccion> listarPorUsuario(int idUsuario);

    /** Progreso del usuario en una lección, bloqueado hasta el fin de la transacción. */
    Optional<ProgresoLeccion> bloquear(Transaccion tx, int idUsuario, int idLeccion);

    void insertar(Transaccion tx, ProgresoLeccion progreso);

    void actualizar(Transaccion tx, ProgresoLeccion progreso);
}
