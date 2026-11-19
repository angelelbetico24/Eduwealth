package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.SQLException;

public class GestorTransaccionesJdbc implements GestorTransacciones {

    @Override
    public Transaccion iniciar() {
        Connection conexion = ConexionBD.obtenerConexion();
        try {
            conexion.setAutoCommit(false);
            return new TransaccionJdbc(conexion);
        } catch (SQLException e) {
            try {
                conexion.close();
            } catch (SQLException ignorada) {
                // se informa del error original
            }
            throw new DataAccessException("No se pudo iniciar la operación.", e);
        }
    }
}
