package com.eduwealth.dao;

/**
 * Error de acceso a datos. Envuelve las {@link java.sql.SQLException} para que
 * las capas superiores no dependan de JDBC (la interfaz solo muestra el mensaje).
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
