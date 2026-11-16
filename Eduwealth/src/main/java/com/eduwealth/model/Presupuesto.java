package com.eduwealth.model;

import java.math.BigDecimal;

/**
 * Límite de gasto mensual de un usuario para una categoría (RF-17).
 */
public class Presupuesto {

    private int idPresupuesto;
    private int idUsuario;
    private int idCategoria;
    private BigDecimal montoLimite;
    private int mes;
    private int anio;

    public Presupuesto() {
    }

    public Presupuesto(int idPresupuesto, int idUsuario, int idCategoria,
                       BigDecimal montoLimite, int mes, int anio) {
        this.idPresupuesto = idPresupuesto;
        this.idUsuario = idUsuario;
        this.idCategoria = idCategoria;
        this.montoLimite = montoLimite;
        this.mes = mes;
        this.anio = anio;
    }

    public int getIdPresupuesto() {
        return idPresupuesto;
    }

    public void setIdPresupuesto(int idPresupuesto) {
        this.idPresupuesto = idPresupuesto;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public BigDecimal getMontoLimite() {
        return montoLimite;
    }

    public void setMontoLimite(BigDecimal montoLimite) {
        this.montoLimite = montoLimite;
    }

    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }
}
