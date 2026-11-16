package com.eduwealth.util;

import com.eduwealth.model.Usuario;

/**
 * Usuario autenticado durante la ejecución de la aplicación (escritorio, un solo usuario a la vez).
 */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuario() {
        if (usuarioActual == null) {
            throw new IllegalStateException("No hay ninguna sesión iniciada.");
        }
        return usuarioActual;
    }

    public static boolean hayUsuario() {
        return usuarioActual != null;
    }
}
