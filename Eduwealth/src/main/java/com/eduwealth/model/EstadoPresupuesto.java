package com.eduwealth.model;

/**
 * Estado de una categoría de presupuesto según RN-03 y C-16:
 * menos del 80 %, del 80 % hasta el 100 % sin alcanzarlo, y 100 % o más.
 */
public enum EstadoPresupuesto {
    DENTRO_LIMITE("Dentro del límite"),
    EN_ALERTA("En Alerta"),
    EXCEDIDO("Presupuesto Excedido");

    private final String etiqueta;

    EstadoPresupuesto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
