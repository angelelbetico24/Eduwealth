package com.eduwealth.dao;

/** Se intentó insertar un registro que viola una restricción UNIQUE (por ejemplo, un email repetido). */
public class RegistroDuplicadoException extends DataAccessException {

    public RegistroDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
