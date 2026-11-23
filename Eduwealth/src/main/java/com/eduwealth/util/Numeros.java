package com.eduwealth.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Lectura de números escritos por el usuario con las convenciones españolas.
 * <ul>
 *   <li>La coma es el separador decimal: {@code 12,5} → 12.5.</li>
 *   <li>El punto agrupa miles si va seguido de grupos de 3 cifras: {@code 1.000} → 1000,
 *       {@code 1.000,50} → 1000.50.</li>
 *   <li>Si no, el punto se acepta como decimal ({@code 0.5}, {@code 12.50}), por comodidad.</li>
 * </ul>
 * Así {@code 1.000} nunca se interpreta en silencio como 1.
 */
public final class Numeros {

    /** 1.000 · 12.345.678 · 1.000,25 (sin cero inicial, para que 0.500 siga siendo 0,5). */
    private static final Pattern CON_MILES = Pattern.compile("[1-9]\\d{0,2}(\\.\\d{3})+(,\\d+)?");

    private Numeros() {
    }

    /**
     * @throws NumberFormatException si el texto no es un número o mezcla los separadores de forma ambigua
     */
    public static BigDecimal leer(String texto) {
        String limpio = texto.strip();
        if (CON_MILES.matcher(limpio).matches()) {
            return new BigDecimal(limpio.replace(".", "").replace(',', '.'));
        }
        if (limpio.contains(".") && limpio.contains(",")) {
            throw new NumberFormatException("Separadores ambiguos: " + limpio);
        }
        return new BigDecimal(limpio.replace(',', '.'));
    }
}
