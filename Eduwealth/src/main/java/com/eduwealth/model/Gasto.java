package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salida de dinero anotada por el usuario, en su divisa base (RF-16, C-17).
 */
public class Gasto {

    private int idGasto;
    private int idUsuario;
    private int idCategoria;
    private BigDecimal monto;
    private LocalDate fecha;
    private String concepto;

    public Gasto() {
    }

    public Gasto(int idGasto, int idUsuario, int idCategoria, BigDecimal monto,
                 LocalDate fecha, String concepto) {
        this.idGasto = idGasto;
        this.idUsuario = idUsuario;
        this.idCategoria = idCategoria;
        this.monto = monto;
        this.fecha = fecha;
        this.concepto = concepto;
    }

    public int getIdGasto() {
        return idGasto;
    }

    public void setIdGasto(int idGasto) {
        this.idGasto = idGasto;
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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }
}
