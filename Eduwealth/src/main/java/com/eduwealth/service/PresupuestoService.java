package com.eduwealth.service;

import com.eduwealth.dao.PresupuestoDAO;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.ResumenCategoria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Presupuestos y control de gastos. Implementa los umbrales de RN-03 (rangos sin huecos, C-16).
 */
public class PresupuestoService {

    public static final BigDecimal UMBRAL_ALERTA = new BigDecimal("80");
    public static final BigDecimal UMBRAL_EXCEDIDO = new BigDecimal("100");

    private final PresupuestoDAO presupuestoDAO;

    public PresupuestoService(PresupuestoDAO presupuestoDAO) {
        this.presupuestoDAO = presupuestoDAO;
    }

    public List<ResumenCategoria> resumenMes(int idUsuario, int mes, int anio) {
        return presupuestoDAO.listarResumenMes(idUsuario, mes, anio);
    }

    /** Porcentaje gastado sobre el límite, con 1 decimal. */
    public BigDecimal porcentaje(BigDecimal gastado, BigDecimal limite) {
        if (limite.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return gastado.multiply(BigDecimal.valueOf(100)).divide(limite, 1, RoundingMode.HALF_UP);
    }

    /**
     * RN-03: &lt; 80 % dentro del límite; ≥ 80 % y &lt; 100 % en alerta; ≥ 100 % excedido.
     * Se compara el porcentaje sin redondear para que, por ejemplo, el 99,96 % siga en alerta.
     */
    public EstadoPresupuesto evaluarUmbral(BigDecimal gastado, BigDecimal limite) {
        BigDecimal cien = BigDecimal.valueOf(100);
        BigDecimal gastadoPor100 = gastado.multiply(cien);
        if (gastadoPor100.compareTo(limite.multiply(UMBRAL_EXCEDIDO)) >= 0) {
            return EstadoPresupuesto.EXCEDIDO;
        }
        if (gastadoPor100.compareTo(limite.multiply(UMBRAL_ALERTA)) >= 0) {
            return EstadoPresupuesto.EN_ALERTA;
        }
        return EstadoPresupuesto.DENTRO_LIMITE;
    }
}
