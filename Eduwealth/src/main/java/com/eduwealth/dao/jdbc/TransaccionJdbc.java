package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.Transaccion;

import java.sql.Connection;
import java.sql.SQLException;

/** Transacción JDBC: una conexión con autocommit desactivado. */
public final class TransaccionJdbc implements Transaccion {

    private final Connection conexion;
    private boolean confirmada;

    TransaccionJdbc(Connection conexion) {
        this.conexion = conexion;
    }

    /** Conexión de la transacción para los DAO JDBC. */
    static Connection conexion(Transaccion tx) {
        if (!(tx instanceof TransaccionJdbc jdbc)) {
            throw new IllegalArgumentException("Se esperaba una transacción JDBC");
        }
        return jdbc.conexion;
    }

    @Override
    public void confirmar() {
        try {
            conexion.commit();
            confirmada = true;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la operación. No se ha realizado ningún cambio.", e);
        }
    }

    @Override
    public void close() {
        try {
            if (!confirmada) {
                conexion.rollback();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al deshacer la operación.", e);
        } finally {
            try {
                conexion.close();
            } catch (SQLException ignorada) {
                // La conexión ya no se usa: no hay nada más que hacer
            }
        }
    }
}
