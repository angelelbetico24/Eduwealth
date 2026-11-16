package com.eduwealth.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Broker real regulado del directorio (RF-14). Los campos de filtrado
 * (comisión mínima, tipos de activo y nivel) proceden de C-22.
 */
public class Broker {

    private int idBroker;
    private String nombre;
    private String descripcion;
    private String comisionesInfo;
    private BigDecimal comisionMinima;
    private Set<TipoActivo> tiposActivo = EnumSet.noneOf(TipoActivo.class);
    private Nivel nivelRecomendado;
    private String regulacion;
    private String urlReferido;

    public Broker() {
    }

    public Broker(int idBroker, String nombre, String descripcion, String comisionesInfo,
                  BigDecimal comisionMinima, Set<TipoActivo> tiposActivo, Nivel nivelRecomendado,
                  String regulacion, String urlReferido) {
        this.idBroker = idBroker;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.comisionesInfo = comisionesInfo;
        this.comisionMinima = comisionMinima;
        setTiposActivo(tiposActivo);
        this.nivelRecomendado = nivelRecomendado;
        this.regulacion = regulacion;
        this.urlReferido = urlReferido;
    }

    public boolean ofrece(TipoActivo tipo) {
        return tiposActivo.contains(tipo);
    }

    public int getIdBroker() {
        return idBroker;
    }

    public void setIdBroker(int idBroker) {
        this.idBroker = idBroker;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getComisionesInfo() {
        return comisionesInfo;
    }

    public void setComisionesInfo(String comisionesInfo) {
        this.comisionesInfo = comisionesInfo;
    }

    public BigDecimal getComisionMinima() {
        return comisionMinima;
    }

    public void setComisionMinima(BigDecimal comisionMinima) {
        this.comisionMinima = comisionMinima;
    }

    public Set<TipoActivo> getTiposActivo() {
        return Collections.unmodifiableSet(tiposActivo);
    }

    public void setTiposActivo(Set<TipoActivo> tiposActivo) {
        this.tiposActivo = (tiposActivo == null || tiposActivo.isEmpty())
                ? EnumSet.noneOf(TipoActivo.class)
                : EnumSet.copyOf(tiposActivo);
    }

    public Nivel getNivelRecomendado() {
        return nivelRecomendado;
    }

    public void setNivelRecomendado(Nivel nivelRecomendado) {
        this.nivelRecomendado = nivelRecomendado;
    }

    public String getRegulacion() {
        return regulacion;
    }

    public void setRegulacion(String regulacion) {
        this.regulacion = regulacion;
    }

    public String getUrlReferido() {
        return urlReferido;
    }

    public void setUrlReferido(String urlReferido) {
        this.urlReferido = urlReferido;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
