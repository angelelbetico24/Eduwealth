package com.eduwealth.controller;

/**
 * Secciones del menú lateral. {@code vista} es null en los módulos aún no implementados.
 */
public enum Seccion {
    INICIO("Panel Principal", "inicio",
            "Resumen de tu patrimonio, presupuestos y progreso."),
    SIMULADOR("Simulador", "simulador",
            "Compra y vende activos con dinero virtual y sigue la rentabilidad de tu cartera."),
    PRESUPUESTOS("Presupuestos", "presupuestos",
            "Define límites de gasto por categoría y recibe alertas al 80 % y al 100 %."),
    EDUCACION("Educación", "educacion",
            "Lecciones por niveles con test de evaluación y medidor de preparación."),
    BROKERS("Brokers", "brokers",
            "Compara brokers regulados y accede a su web oficial cuando estés preparado."),
    CONFIGURACION("Configuración", "configuracion",
            "Cambia tu moneda base, tu contraseña o reinicia el simulador.");

    private final String titulo;
    private final String vista;
    private final String descripcion;

    Seccion(String titulo, String vista, String descripcion) {
        this.titulo = titulo;
        this.vista = vista;
        this.descripcion = descripcion;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getVista() {
        return vista;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean estaDisponible() {
        return vista != null;
    }
}
