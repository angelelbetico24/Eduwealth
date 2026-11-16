package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Compra o venta ficticia. {@code pnlRealizado} solo tiene valor en las ventas,
 * expresado en la divisa del activo (C-04).
 */
public class OperacionSimulada {

    private int idOperacion;
    private int idUsuario;
    private int idActivo;
    private TipoOperacion tipoOperacion;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal pnlRealizado;
    private LocalDateTime fechaHora;

    public OperacionSimulada() {
    }

    public OperacionSimulada(int idOperacion, int idUsuario, int idActivo, TipoOperacion tipoOperacion,
                             BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal pnlRealizado,
                             LocalDateTime fechaHora) {
        this.idOperacion = idOperacion;
        this.idUsuario = idUsuario;
        this.idActivo = idActivo;
        this.tipoOperacion = tipoOperacion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.pnlRealizado = pnlRealizado;
        this.fechaHora = fechaHora;
    }

    /** Importe total de la operación (cantidad × precio) en la divisa del activo. */
    public BigDecimal getImporteTotal() {
        return cantidad.multiply(precioUnitario);
    }

    public int getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(int idOperacion) {
        this.idOperacion = idOperacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdActivo() {
        return idActivo;
    }

    public void setIdActivo(int idActivo) {
        this.idActivo = idActivo;
    }

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(TipoOperacion tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPnlRealizado() {
        return pnlRealizado;
    }

    public void setPnlRealizado(BigDecimal pnlRealizado) {
        this.pnlRealizado = pnlRealizado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
