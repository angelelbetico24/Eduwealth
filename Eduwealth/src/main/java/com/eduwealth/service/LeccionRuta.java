package com.eduwealth.service;

import com.eduwealth.model.Leccion;

import java.math.BigDecimal;

/**
 * Lección dentro de la ruta de aprendizaje del usuario.
 *
 * @param mejorNota mejor nota obtenida (0-100) o null si nunca ha hecho el test
 */
public record LeccionRuta(Leccion leccion, int preguntas, int minutos, Disponibilidad disponibilidad,
                          BigDecimal mejorNota) {
}
