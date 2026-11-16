package com.eduwealth.model;

import java.math.BigDecimal;

/**
 * Producto financiero cotizado en el simulador. El precio está en la divisa del activo.
 */
public class Activo {

    private int idActivo;
    private String ticker;
    private String nombre;
    private TipoActivo tipoActivo;
    private BigDecimal precioActual;
    private int idDivisa;

    public Activo() {
    }

    public Activo(int idActivo, String ticker, String nombre, TipoActivo tipoActivo,
                  BigDecimal precioActual, int idDivisa) {
        this.idActivo = idActivo;
        this.ticker = ticker;
        this.nombre = nombre;
        this.tipoActivo = tipoActivo;
        this.precioActual = precioActual;
        this.idDivisa = idDivisa;
    }

    public int getIdActivo() {
        return idActivo;
    }

    public void setIdActivo(int idActivo) {
        this.idActivo = idActivo;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoActivo getTipoActivo() {
        return tipoActivo;
    }

    public void setTipoActivo(TipoActivo tipoActivo) {
        this.tipoActivo = tipoActivo;
    }

    public BigDecimal getPrecioActual() {
        return precioActual;
    }

    public void setPrecioActual(BigDecimal precioActual) {
        this.precioActual = precioActual;
    }

    public int getIdDivisa() {
        return idDivisa;
    }

    public void setIdDivisa(int idDivisa) {
        this.idDivisa = idDivisa;
    }

    @Override
    public String toString() {
        return ticker + " - " + nombre;
    }
}
