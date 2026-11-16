package com.eduwealth.service;

/**
 * Datos de entrada no válidos o regla de negocio incumplida. El mensaje está
 * pensado para mostrarse al usuario; {@code campo} indica qué control marcar
 * en rojo (puede ser {@code null}).
 */
public class ValidacionException extends RuntimeException {

    private final String campo;

    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public ValidacionException(String mensaje) {
        this(null, mensaje);
    }

    public String getCampo() {
        return campo;
    }
}
