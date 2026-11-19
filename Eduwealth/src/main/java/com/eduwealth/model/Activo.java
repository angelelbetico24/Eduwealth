package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto financiero cotizado en el simulador. El precio está en la divisa del activo.
 * {@code fechaActualizacion} es la última vez que cambió el precio (movimiento simulado del mercado, C-26).
 */
public class Activo {

    private int idActivo;
    private String ticker;
    private String nombre;
    private TipoActivo tipoActivo;
    private BigDecimal precioActual;
    private int idDivisa;
    private LocalDateTime fechaActualizacion;

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

    public Activo(int idActivo, String ticker, String nombre, TipoActivo tipoActivo,
                  BigDecimal precioActual, int idDivisa, LocalDateTime fechaActualizacion) {
        this(idActivo, ticker, nombre, tipoActivo, precioActual, idDivisa);
        this.fechaActualizacion = fechaActualizacion;
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

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public String toString() {
        return ticker + " - " + nombre;
    }
}
