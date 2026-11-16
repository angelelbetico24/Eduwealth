package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.model.ResumenCategoria;
import com.eduwealth.model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Construye el resumen del Panel Principal combinando simulador, presupuestos, educación y brokers.
 * Solo lee datos: no modifica nada.
 */
public class PanelService {

    private final DivisaService divisaService;
    private final PresupuestoService presupuestoService;
    private final EducacionService educacionService;
    private final PosicionCarteraDAO posicionDAO;
    private final ActivoDAO activoDAO;
    private final OperacionSimuladaDAO operacionDAO;
    private final BrokerDAO brokerDAO;

    public PanelService(DivisaService divisaService, PresupuestoService presupuestoService,
                        EducacionService educacionService, PosicionCarteraDAO posicionDAO,
                        ActivoDAO activoDAO, OperacionSimuladaDAO operacionDAO, BrokerDAO brokerDAO) {
        this.divisaService = divisaService;
        this.presupuestoService = presupuestoService;
        this.educacionService = educacionService;
        this.posicionDAO = posicionDAO;
        this.activoDAO = activoDAO;
        this.operacionDAO = operacionDAO;
        this.brokerDAO = brokerDAO;
    }

    public ResumenPanel construirResumen(Usuario usuario, LocalDate hoy) {
        // Catálogos pequeños: se cargan una vez en memoria en lugar de consultar por cada posición.
        Map<Integer, Divisa> divisas = divisaService.listarDivisas().stream()
                .collect(Collectors.toMap(Divisa::getIdDivisa, Function.identity()));
        Map<Integer, Activo> activos = activoDAO.listarTodos().stream()
                .collect(Collectors.toMap(Activo::getIdActivo, Function.identity()));
        Divisa base = divisas.get(usuario.getIdDivisaBase());

        List<ResumenPanel.FilaCartera> filas = new ArrayList<>();
        BigDecimal valorCartera = BigDecimal.ZERO;
        boolean desactualizadas = false;
        LocalDateTime ahora = hoy.atStartOfDay();
        for (PosicionCartera posicion : posicionDAO.listarPorUsuario(usuario.getIdUsuario())) {
            Activo activo = activos.get(posicion.getIdActivo());
            Divisa divisaActivo = divisas.get(activo.getIdDivisa());
            BigDecimal precio = activo.getPrecioActual();
            filas.add(new ResumenPanel.FilaCartera(activo.getTicker(), activo.getNombre(),
                    posicion.getCantidadTitulos(), posicion.getPrecioMedioCompra(), precio,
                    posicion.calcularPnLNoRealizado(precio), posicion.calcularPorcentajePnL(precio),
                    divisaActivo.getSimbolo()));
            // RN-04: el valor de cada posición se convierte a la divisa base antes de sumarlo
            valorCartera = valorCartera.add(
                    divisaService.convertir(posicion.calcularValorActual(precio), divisaActivo, base));
            desactualizadas |= divisaActivo.getIdDivisa() != base.getIdDivisa()
                    && divisaService.estaDesactualizada(divisaActivo, ahora);
        }

        BigDecimal saldo = usuario.getSaldoFicticio();
        BigDecimal patrimonio = saldo.add(valorCartera);
        BigDecimal capitalInicial = AutenticacionService.SALDO_INICIAL;
        BigDecimal rentabilidad = patrimonio.subtract(capitalInicial);
        BigDecimal rentabilidadPct = rentabilidad.multiply(BigDecimal.valueOf(100))
                .divide(capitalInicial, 2, RoundingMode.HALF_UP);

        ResumenPanel.Educacion educacion = resumenEducacion(usuario);
        return new ResumenPanel(base, saldo, valorCartera, patrimonio, rentabilidad, rentabilidadPct, filas,
                resumenPresupuestos(usuario, hoy), educacion, recomendarBroker(educacion), desactualizadas);
    }

    private ResumenPanel.Presupuestos resumenPresupuestos(Usuario usuario, LocalDate hoy) {
        List<ResumenCategoria> categorias =
                presupuestoService.resumenMes(usuario.getIdUsuario(), hoy.getMonthValue(), hoy.getYear());
        BigDecimal limite = BigDecimal.ZERO;
        BigDecimal gastado = BigDecimal.ZERO;
        List<ResumenPanel.Aviso> avisos = new ArrayList<>();
        for (ResumenCategoria c : categorias) {
            limite = limite.add(c.limite());
            gastado = gastado.add(c.gastado());
            EstadoPresupuesto estado = presupuestoService.evaluarUmbral(c.gastado(), c.limite());
            if (estado != EstadoPresupuesto.DENTRO_LIMITE) {
                avisos.add(new ResumenPanel.Aviso(c.categoria(), c.gastado(), c.limite(),
                        presupuestoService.porcentaje(c.gastado(), c.limite()), estado));
            }
        }
        EstadoPresupuesto estadoTotal = categorias.isEmpty() ? null : presupuestoService.evaluarUmbral(gastado, limite);
        return new ResumenPanel.Presupuestos(limite, gastado, presupuestoService.porcentaje(gastado, limite),
                estadoTotal, categorias.size(), avisos);
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
