package com.eduwealth.service;

import com.eduwealth.dao.LeccionDAO;
import com.eduwealth.dao.ProgresoLeccionDAO;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.ProgresoLeccion;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Módulo educativo. De momento: medidor de preparación (RN-07) y siguiente lección pendiente.
 */
public class EducacionService {

    /** Preparación a partir de la cual el usuario está «Listo para la inversión real» (RN-07). */
    public static final int UMBRAL_LISTO = 75;
    private static final int PESO_EDUCACION = 70;
    private static final int PESO_SIMULADOR = 30;
    private static final int OPERACIONES_OBJETIVO = 10;

    private final LeccionDAO leccionDAO;
    private final ProgresoLeccionDAO progresoDAO;

    public EducacionService(LeccionDAO leccionDAO, ProgresoLeccionDAO progresoDAO) {
        this.leccionDAO = leccionDAO;
        this.progresoDAO = progresoDAO;
    }

    public List<Leccion> listarLecciones() {
        return leccionDAO.listarTodas();
    }

    /** Identificadores de las lecciones aprobadas (nota ≥ 70 %, RN-05) por el usuario. */
    public Set<Integer> leccionesAprobadas(int idUsuario) {
        return progresoDAO.listarPorUsuario(idUsuario).stream()
                .filter(ProgresoLeccion::isCompletada)
                .map(ProgresoLeccion::getIdLeccion)
                .collect(Collectors.toSet());
    }

    /**
     * RN-07: {@code 70 × (aprobadas ÷ totales) + 30 × min(operaciones ÷ 10, 1)}, redondeado a entero.
     * Sin lecciones cargadas, la parte educativa vale 0 (C-12).
     */
    public int calcularPreparacion(int aprobadas, int totales, int operaciones) {
        double educacion = totales == 0 ? 0 : (double) aprobadas / totales;
        double simulador = Math.min((double) operaciones / OPERACIONES_OBJETIVO, 1.0);
        return (int) Math.round(PESO_EDUCACION * educacion + PESO_SIMULADOR * simulador);
    }

    public boolean estaListo(int preparacion) {
        return preparacion >= UMBRAL_LISTO;
    }

    /** Primera lección, en orden de nivel, que el usuario aún no ha aprobado. */
    public Optional<Leccion> siguienteLeccion(List<Leccion> lecciones, Set<Integer> aprobadas) {
        return lecciones.stream().filter(l -> !aprobadas.contains(l.getIdLeccion())).findFirst();
    }
}
