package com.eduwealth.model;

/**
 * Lección teórica del módulo educativo (RF-11).
 */
public class Leccion {

    private int idLeccion;
    private String titulo;
    private String contenido;
    private Nivel nivel;

    public Leccion() {
    }

    public Leccion(int idLeccion, String titulo, String contenido, Nivel nivel) {
        this.idLeccion = idLeccion;
        this.titulo = titulo;
        this.contenido = contenido;
        this.nivel = nivel;
    }

    public int getIdLeccion() {
        return idLeccion;
    }

    public void setIdLeccion(int idLeccion) {
        this.idLeccion = idLeccion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public void setNivel(Nivel nivel) {
        this.nivel = nivel;
    }

    @Override
    public String toString() {
        return titulo + " [" + nivel + "]";
    }
}
