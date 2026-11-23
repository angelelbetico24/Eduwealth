package com.eduwealth.service;

import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Usuario;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Construye el resumen del Panel Principal combinando simulador, presupuestos, educación y brokers.
 * Solo lee datos: no modifica nada.
 */
public class PanelService {

    private final SimuladorService simuladorService;
    private final PresupuestoService presupuestoService;
    private final EducacionService educacionService;
    private final BrokerDAO brokerDAO;

    public PanelService(SimuladorService simuladorService, PresupuestoService presupuestoService,
                        EducacionService educacionService, BrokerDAO brokerDAO) {
        this.simuladorService = simuladorService;
        this.presupuestoService = presupuestoService;
        this.educacionService = educacionService;
        this.brokerDAO = brokerDAO;
    }

    public ResumenPanel construirResumen(Usuario usuario, LocalDate hoy) {
        ResumenEducacion educacion = educacionService.resumen(usuario.getIdUsuario());
        return new ResumenPanel(
                simuladorService.resumenCartera(usuario, hoy.atStartOfDay()),
                presupuestoService.resumenDelMes(usuario.getIdUsuario(), YearMonth.from(hoy)),
                educacion,
                recomendarBroker(educacion));
    }

    /** Broker cuyo nivel recomendado coincide con el nivel en el que está formándose el usuario. */
    private Broker recomendarBroker(ResumenEducacion educacion) {
        Nivel nivelUsuario = educacion.nivelActual();
        List<Broker> brokers = brokerDAO.listarTodos();
        return brokers.stream().filter(b -> b.getNivelRecomendado() == nivelUsuario).findFirst()
                .orElse(brokers.isEmpty() ? null : brokers.get(0));
    }
}
