package com.eduwealth.service;

import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.model.Divisa;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Consulta de divisas y conversión a la moneda base del usuario (RN-04).
 * El EUR es la moneda pivote: {@code tipoCambioBase} = unidades por 1 EUR (C-06).
 */
public class DivisaService {

    /** Antigüedad a partir de la cual un tipo de cambio se considera desactualizado (C-08). */
    public static final Duration ANTIGUEDAD_MAXIMA_TIPO_CAMBIO = Duration.ofDays(7);

    private final DivisaDAO divisaDAO;

    public DivisaService(DivisaDAO divisaDAO) {
        this.divisaDAO = divisaDAO;
    }

    public List<Divisa> listarDivisas() {
        return divisaDAO.listarTodas();
    }

    public Optional<Divisa> buscarPorId(int idDivisa) {
        return divisaDAO.buscarPorId(idDivisa);
    }

    /**
     * RN-04: convierte un importe de la divisa {@code origen} a la divisa {@code destino}:
     * {@code importe ÷ tc_origen × tc_destino}. Se mantiene la precisión; el redondeo se hace al mostrar.
     */
    public BigDecimal convertir(BigDecimal importe, Divisa origen, Divisa destino) {
        if (origen.getIdDivisa() == destino.getIdDivisa()) {
            return importe;
        }
        return importe.divide(origen.getTipoCambioBase(), MathContext.DECIMAL64)
                .multiply(destino.getTipoCambioBase(), MathContext.DECIMAL64);
    }

    /** C-08: indica si el tipo de cambio lleva más de 7 días sin actualizarse. */
    public boolean estaDesactualizada(Divisa divisa, LocalDateTime ahora) {
        return divisa.getFechaActualizacion() != null
                && divisa.getFechaActualizacion().plus(ANTIGUEDAD_MAXIMA_TIPO_CAMBIO).isBefore(ahora);
    }
}
