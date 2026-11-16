package com.eduwealth.service;

import com.eduwealth.dao.jdbc.DivisaDAOJdbc;
import com.eduwealth.dao.jdbc.UsuarioDAOJdbc;

/**
 * Fábrica de servicios con inyección de dependencias manual (apartado 6.11.2).
 * Es el único punto donde se eligen las implementaciones JDBC de los DAO:
 * los controladores piden servicios y nunca crean DAO ni ejecutan SQL.
 */
public final class FabricaServicios {

    private static final AutenticacionService AUTENTICACION = new AutenticacionService(new UsuarioDAOJdbc());
    private static final DivisaService DIVISAS = new DivisaService(new DivisaDAOJdbc());

    private FabricaServicios() {
    }

    public static AutenticacionService autenticacion() {
        return AUTENTICACION;
    }

    public static DivisaService divisas() {
        return DIVISAS;
    }
}
