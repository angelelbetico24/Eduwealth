package com.eduwealth.service;

import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;

import java.math.BigDecimal;

/**
 * Progreso educativo y medidor de preparación (RN-07).
 *
 * @param notaMedia        media de las mejores notas de las lecciones intentadas, sobre 10 (null si ninguna)
 * @param siguienteLeccion primera lección pendiente en orden de nivel (null si las ha aprobado todas)
 * @param nivelActual      nivel en el que se está formando el usuario
 */
public record ResumenEducacion(int aprobadas, int totales, int preparacion, boolean listo, BigDecimal notaMedia,
                               Leccion siguienteLeccion, Nivel nivelActual) {
}
