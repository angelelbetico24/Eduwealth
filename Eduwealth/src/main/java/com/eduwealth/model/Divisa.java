package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Divisa soportada. {@code tipoCambioBase} son las unidades de esta divisa
 * por 1 EUR, que actúa como moneda pivote (C-06).
 */
public class Divisa {

    private int idDivisa;
    private String codigo;
    private String nombre;
    private String simbolo;
    private BigDecimal tipoCambioBase;
    private LocalDateTime fechaActualizacion;

    public Divisa() {
    }

    public Divisa(int idDivisa, String codigo, String nombre, String simbolo,
                  BigDecimal tipoCambioBase, LocalDateTime fechaActualizacion) {
        this.idDivisa = idDivisa;
        this.codigo = codigo;
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.tipoCambioBase = tipoCambioBase;
        this.fechaActualizacion = fechaActualizacion;
    }

    public int getIdDivisa() {
        return idDivisa;
    }

    public void setIdDivisa(int idDivisa) {
        this.idDivisa = idDivisa;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public BigDecimal getTipoCambioBase() {
        return tipoCambioBase;
    }

    public void setTipoCambioBase(BigDecimal tipoCambioBase) {
        this.tipoCambioBase = tipoCambioBase;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + simbolo + ")";
    }
}
