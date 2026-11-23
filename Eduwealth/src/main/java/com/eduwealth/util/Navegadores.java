package com.eduwealth.util;

import javafx.application.HostServices;

import java.net.URI;

/**
 * Abre enlaces en el navegador predeterminado del sistema (CU-09: la operativa real se hace
 * siempre fuera de EduWealth, RN-06). Usa los HostServices de JavaFX registrados al arrancar.
 */
public final class Navegadores {

    private static HostServices servicios;

    private Navegadores() {
    }

    public static void inicializar(HostServices hostServices) {
        servicios = hostServices;
    }

    public static void abrir(URI enlace) {
        if (servicios == null) {
            throw new IllegalStateException("La aplicación no ha registrado los servicios del sistema.");
        }
        servicios.showDocument(enlace.toString());
    }
}
