package com.eduwealth.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lectura de números con convenciones españolas (detectado en la revisión: «1.000» se leía como 1). */
class NumerosTest {

    @Test
    void elPuntoDeMilesNoSeConfundeConElDecimal() {
        assertEquals(0, new BigDecimal("1000").compareTo(Numeros.leer("1.000")));
        assertEquals(0, new BigDecimal("1000.50").compareTo(Numeros.leer("1.000,50")));
        assertEquals(0, new BigDecimal("12345678").compareTo(Numeros.leer("12.345.678")));
    }

    @Test
    void comaOPuntoComoDecimal() {
        assertEquals(0, new BigDecimal("12.5").compareTo(Numeros.leer(" 12,5 ")));
        assertEquals(0, new BigDecimal("0.5").compareTo(Numeros.leer("0.5")));
        assertEquals(0, new BigDecimal("0.5").compareTo(Numeros.leer("0.500"))); // un 0 inicial no agrupa miles
        assertEquals(0, new BigDecimal("12.50").compareTo(Numeros.leer("12.50")));
        assertEquals(0, new BigDecimal("25").compareTo(Numeros.leer("25")));
    }

    @Test
    void textosNoValidosOAmbiguos() {
        assertThrows(NumberFormatException.class, () -> Numeros.leer("doce"));
        assertThrows(NumberFormatException.class, () -> Numeros.leer("1,000.50"));
        assertThrows(NumberFormatException.class, () -> Numeros.leer("1.00,5"));
    }
}
