package com.eduwealth.service;

import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.model.Divisa;

import java.util.List;
import java.util.Optional;

/**
 * Consulta de divisas. Más adelante incluirá la conversión a moneda base (RN-04).
 */
public class DivisaService {

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
}
