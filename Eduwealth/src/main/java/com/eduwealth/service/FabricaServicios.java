package com.eduwealth.service;

import com.eduwealth.dao.jdbc.ActivoDAOJdbc;
import com.eduwealth.dao.jdbc.BrokerDAOJdbc;
import com.eduwealth.dao.jdbc.CategoriaGastoDAOJdbc;
import com.eduwealth.dao.jdbc.DivisaDAOJdbc;
import com.eduwealth.dao.jdbc.GastoDAOJdbc;
import com.eduwealth.dao.jdbc.GestorTransaccionesJdbc;
import com.eduwealth.dao.jdbc.LeccionDAOJdbc;
import com.eduwealth.dao.jdbc.OperacionSimuladaDAOJdbc;
import com.eduwealth.dao.jdbc.PosicionCarteraDAOJdbc;
import com.eduwealth.dao.jdbc.PresupuestoDAOJdbc;
import com.eduwealth.dao.jdbc.ProgresoLeccionDAOJdbc;
import com.eduwealth.dao.jdbc.UsuarioDAOJdbc;

import java.util.Random;

/**
 * Fábrica de servicios con inyección de dependencias manual (apartado 6.11.2).
 * Es el único punto donde se eligen las implementaciones JDBC de los DAO:
 * los controladores piden servicios y nunca crean DAO ni ejecutan SQL.
 */
public final class FabricaServicios {

    private static final AutenticacionService AUTENTICACION = new AutenticacionService(new UsuarioDAOJdbc());
    private static final DivisaService DIVISAS = new DivisaService(new DivisaDAOJdbc());
    private static final PresupuestoService PRESUPUESTOS = new PresupuestoService(
            new PresupuestoDAOJdbc(), new GastoDAOJdbc(), new CategoriaGastoDAOJdbc());
    private static final EducacionService EDUCACION =
            new EducacionService(new LeccionDAOJdbc(), new ProgresoLeccionDAOJdbc());
    private static final SimuladorService SIMULADOR = new SimuladorService(new UsuarioDAOJdbc(),
            new ActivoDAOJdbc(), new PosicionCarteraDAOJdbc(), new OperacionSimuladaDAOJdbc(),
            new GestorTransaccionesJdbc(), DIVISAS);
    private static final MercadoService MERCADO =
            new MercadoService(new ActivoDAOJdbc(), new GestorTransaccionesJdbc(), new Random());
    private static final PanelService PANEL = new PanelService(SIMULADOR, PRESUPUESTOS, EDUCACION,
            new OperacionSimuladaDAOJdbc(), new BrokerDAOJdbc());

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

    public static SimuladorService simulador() {
        return SIMULADOR;
    }

    public static MercadoService mercado() {
        return MERCADO;
    }

    public static PanelService panel() {
        return PANEL;
    }
}
