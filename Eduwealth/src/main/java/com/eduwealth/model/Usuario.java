package com.eduwealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usuario de la aplicación. El saldo ficticio y el capital inicial se expresan en su divisa base
 * (C-07, C-28): el capital inicial es la referencia para calcular la rentabilidad del simulador.
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String email;
    private String passwordHash;
    private Rol rol;
    private BigDecimal saldoFicticio;
    private BigDecimal capitalInicial;
    private int idDivisaBase;
    private LocalDateTime fechaRegistro;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String email, String passwordHash, Rol rol,
                   BigDecimal saldoFicticio, int idDivisaBase, LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.saldoFicticio = saldoFicticio;
        this.capitalInicial = saldoFicticio;
        this.idDivisaBase = idDivisaBase;
        this.fechaRegistro = fechaRegistro;
    }

    public Usuario(int idUsuario, String nombre, String email, String passwordHash, Rol rol,
                   BigDecimal saldoFicticio, BigDecimal capitalInicial, int idDivisaBase,
                   LocalDateTime fechaRegistro) {
        this(idUsuario, nombre, email, passwordHash, rol, saldoFicticio, idDivisaBase, fechaRegistro);
        this.capitalInicial = capitalInicial;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public BigDecimal getSaldoFicticio() {
        return saldoFicticio;
    }

    public void setSaldoFicticio(BigDecimal saldoFicticio) {
        this.saldoFicticio = saldoFicticio;
    }

    public BigDecimal getCapitalInicial() {
        return capitalInicial;
    }

    public void setCapitalInicial(BigDecimal capitalInicial) {
        this.capitalInicial = capitalInicial;
    }

    public int getIdDivisaBase() {
        return idDivisaBase;
    }

    public void setIdDivisaBase(int idDivisaBase) {
        this.idDivisaBase = idDivisaBase;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return nombre + " <" + email + "> [" + rol + "]";
    }
}
