package com.eduwealth.service;

import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Pregunta;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de un test (CU-08).
 *
 * @param nota               porcentaje de aciertos con 2 decimales
 * @param aprobado           nota ≥ 70 (RN-05)
 * @param mejorNota          mejor nota guardada tras este intento (C-10)
 * @param nuevaMejorNota     si este intento ha mejorado la nota guardada
 * @param nivelDesbloqueado  nivel que se ha desbloqueado con este aprobado (RN-08) o null
 */
public record ResultadoTest(Leccion leccion, int aciertos, int total, BigDecimal nota, boolean aprobado,
                            BigDecimal mejorNota, boolean nuevaMejorNota, Nivel nivelDesbloqueado,
                            List<Correccion> correcciones) {

    /** Respuesta del usuario a una pregunta y si ha acertado. */
    public record Correccion(Pregunta pregunta, char elegida, boolean acierto) {
    }
}
