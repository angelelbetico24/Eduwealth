package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Progreso de un usuario en una lección. Guarda la mejor nota obtenida (C-10).
 */
public class ProgresoLeccion {

    private int idProgreso;
    private int idUsuario;
    private int idLeccion;
    private boolean completada;
    private BigDecimal notaTest;
    private LocalDateTime fechaCompletado;

    public ProgresoLeccion() {
    }

    public ProgresoLeccion(int idProgreso, int idUsuario, int idLeccion, boolean completada,
                           BigDecimal notaTest, LocalDateTime fechaCompletado) {
        this.idProgreso = idProgreso;
        this.idUsuario = idUsuario;
        this.idLeccion = idLeccion;
        this.completada = completada;
        this.notaTest = notaTest;
        this.fechaCompletado = fechaCompletado;
    }

    public int getIdProgreso() {
        return idProgreso;
    }

    public void setIdProgreso(int idProgreso) {
        this.idProgreso = idProgreso;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdLeccion() {
        return idLeccion;
    }

    public void setIdLeccion(int idLeccion) {
        this.idLeccion = idLeccion;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public BigDecimal getNotaTest() {
        return notaTest;
    }

    public void setNotaTest(BigDecimal notaTest) {
        this.notaTest = notaTest;
    }

    public LocalDateTime getFechaCompletado() {
        return fechaCompletado;
    }

    public void setFechaCompletado(LocalDateTime fechaCompletado) {
        this.fechaCompletado = fechaCompletado;
    }
}
