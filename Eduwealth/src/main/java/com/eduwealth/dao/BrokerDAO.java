package com.eduwealth.dao;

import com.eduwealth.model.Broker;

import java.util.List;

/** Acceso a datos de la tabla {@code brokers} (directorio, RF-14). */
public interface BrokerDAO {

    List<Broker> listarTodos();

    /** Inserta el broker y devuelve el identificador generado. */
    int insertar(Broker broker);

    void actualizar(Broker broker);

    boolean eliminar(int idBroker);
}
