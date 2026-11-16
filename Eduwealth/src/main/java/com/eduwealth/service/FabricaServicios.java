package com.eduwealth.service;

import com.eduwealth.dao.jdbc.ActivoDAOJdbc;
import com.eduwealth.dao.jdbc.BrokerDAOJdbc;
import com.eduwealth.dao.jdbc.DivisaDAOJdbc;
import com.eduwealth.dao.jdbc.LeccionDAOJdbc;
import com.eduwealth.dao.jdbc.OperacionSimuladaDAOJdbc;
import com.eduwealth.dao.jdbc.PosicionCarteraDAOJdbc;
import com.eduwealth.dao.jdbc.PresupuestoDAOJdbc;
import com.eduwealth.dao.jdbc.ProgresoLeccionDAOJdbc;
import com.eduwealth.dao.jdbc.UsuarioDAOJdbc;

/**
 * Fábrica de servicios con inyección de dependencias manual (apartado 6.11.2).
 * Es el único punto donde se eligen las implementaciones JDBC de los DAO:
 * los controladores piden servicios y nunca crean DAO ni ejecutan SQL.
 */
public final class FabricaServicios {

    private static final AutenticacionService AUTENTICACION = new AutenticacionService(new UsuarioDAOJdbc());
    private static final DivisaService DIVISAS = new DivisaService(new DivisaDAOJdbc());
    private static final PresupuestoService PRESUPUESTOS = new PresupuestoService(new PresupuestoDAOJdbc());
    private static final EducacionService EDUCACION =
            new EducacionService(new LeccionDAOJdbc(), new ProgresoLeccionDAOJdbc());
    private static final PanelService PANEL = new PanelService(DIVISAS, PRESUPUESTOS, EDUCACION,
            new PosicionCarteraDAOJdbc(), new ActivoDAOJdbc(), new OperacionSimuladaDAOJdbc(), new BrokerDAOJdbc());

    private FabricaServicios() {
    }

    public static AutenticacionService autenticacion() {
        return AUTENTICACION;
    }

    public static DivisaService divisas() {
        return DIVISAS;
    }

    public static PresupuestoService presupuestos() {
        return PRESUPUESTOS;
    }

    public static EducacionService educacion() {
        return EDUCACION;
    }

    public static PanelService panel() {
        return PANEL;
    }
}
