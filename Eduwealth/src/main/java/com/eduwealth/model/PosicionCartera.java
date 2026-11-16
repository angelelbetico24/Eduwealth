package com.eduwealth.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Títulos acumulados de un activo en la cartera de un usuario y su precio medio
 * de compra (PMC). Los importes están en la divisa del activo.
 */
public class PosicionCartera {

    private int idPosicion;
    private int idUsuario;
    private int idActivo;
    private BigDecimal cantidadTitulos;
    private BigDecimal precioMedioCompra;

    public PosicionCartera() {
    }

    public PosicionCartera(int idPosicion, int idUsuario, int idActivo,
                           BigDecimal cantidadTitulos, BigDecimal precioMedioCompra) {
        this.idPosicion = idPosicion;
        this.idUsuario = idUsuario;
        this.idActivo = idActivo;
        this.cantidadTitulos = cantidadTitulos;
        this.precioMedioCompra = precioMedioCompra;
    }

    /** Valor de mercado de la posición: títulos × precio actual. */
    public BigDecimal calcularValorActual(BigDecimal precioMercado) {
        return cantidadTitulos.multiply(precioMercado);
    }

    /** Coste total de adquisición: títulos × PMC. */
    public BigDecimal calcularCosteTotal() {
        return cantidadTitulos.multiply(precioMedioCompra);
    }

    /** Ganancia o pérdida no realizada: valor actual − coste. */
    public BigDecimal calcularPnLNoRealizado(BigDecimal precioMercado) {
        return calcularValorActual(precioMercado).subtract(calcularCosteTotal());
    }

    /** Ganancia o pérdida no realizada en porcentaje sobre el coste (2 decimales). */
    public BigDecimal calcularPorcentajePnL(BigDecimal precioMercado) {
        BigDecimal coste = calcularCosteTotal();
        if (coste.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return calcularPnLNoRealizado(precioMercado)
                .multiply(BigDecimal.valueOf(100))
                .divide(coste, 2, RoundingMode.HALF_UP);
    }

    public int getIdPosicion() {
        return idPosicion;
    }

    public void setIdPosicion(int idPosicion) {
        this.idPosicion = idPosicion;
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

    public BigDecimal getCantidadTitulos() {
        return cantidadTitulos;
    }

    public void setCantidadTitulos(BigDecimal cantidadTitulos) {
        this.cantidadTitulos = cantidadTitulos;
    }

    public BigDecimal getPrecioMedioCompra() {
        return precioMedioCompra;
    }

    public void setPrecioMedioCompra(BigDecimal precioMedioCompra) {
        this.precioMedioCompra = precioMedioCompra;
    }
}
