package com.eduwealth.service;

import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Usuario;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Construye el resumen del Panel Principal combinando simulador, presupuestos, educación y brokers.
 * Solo lee datos: no modifica nada.
 */
public class PanelService {

    private final SimuladorService simuladorService;
    private final PresupuestoService presupuestoService;
    private final EducacionService educacionService;
    private final OperacionSimuladaDAO operacionDAO;
    private final BrokerDAO brokerDAO;

    public PanelService(SimuladorService simuladorService, PresupuestoService presupuestoService,
                        EducacionService educacionService, OperacionSimuladaDAO operacionDAO, BrokerDAO brokerDAO) {
        this.simuladorService = simuladorService;
        this.presupuestoService = presupuestoService;
        this.educacionService = educacionService;
        this.operacionDAO = operacionDAO;
        this.brokerDAO = brokerDAO;
    }

    public ResumenPanel construirResumen(Usuario usuario, LocalDate hoy) {
        ResumenPanel.Educacion educacion = resumenEducacion(usuario);
        return new ResumenPanel(
                simuladorService.resumenCartera(usuario, hoy.atStartOfDay()),
                presupuestoService.resumenDelMes(usuario.getIdUsuario(), YearMonth.from(hoy)),
                educacion,
                recomendarBroker(educacion));
    }

    private ResumenPanel.Educacion resumenEducacion(Usuario usuario) {
        List<Leccion> lecciones = educacionService.listarLecciones();
        Set<Integer> aprobadas = educacionService.leccionesAprobadas(usuario.getIdUsuario());
        int operaciones = operacionDAO.contarPorUsuario(usuario.getIdUsuario());
        int preparacion = educacionService.calcularPreparacion(aprobadas.size(), lecciones.size(), operaciones);
        return new ResumenPanel.Educacion(aprobadas.size(), lecciones.size(), preparacion,
                educacionService.estaListo(preparacion),
                educacionService.siguienteLeccion(lecciones, aprobadas).orElse(null));
    }

    /** Broker cuyo nivel recomendado coincide con el nivel en el que está formándose el usuario. */
    private Broker recomendarBroker(ResumenPanel.Educacion educacion) {
        Nivel nivelUsuario = educacion.siguienteLeccion() != null
                ? educacion.siguienteLeccion().getNivel()
                : Nivel.AVANZADO;
        List<Broker> brokers = brokerDAO.listarTodos();
        return brokers.stream().filter(b -> b.getNivelRecomendado() == nivelUsuario).findFirst()
                .orElse(brokers.isEmpty() ? null : brokers.get(0));
    }
}
