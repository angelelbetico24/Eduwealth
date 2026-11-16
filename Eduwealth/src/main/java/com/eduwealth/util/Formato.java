package com.eduwealth.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de importes para la interfaz, con convenciones españolas: 10.000,00 €.
 */
public final class Formato {

    private static final DecimalFormatSymbols SIMBOLOS_ES =
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-ES"));

    private Formato() {
    }

    public static String moneda(BigDecimal importe, String simbolo) {
        // Patrón explícito: agrupa siempre los miles (es-ES no lo hace por defecto en números de 4 cifras)
        DecimalFormat formato = new DecimalFormat("#,##0.00", SIMBOLOS_ES);
        return formato.format(importe) + " " + simbolo;
    }

    /** Porcentaje con coma decimal y signo opcional: 85,0 % · +4,25 %. */
    public static String porcentaje(BigDecimal valor, boolean conSigno) {
        DecimalFormat formato = new DecimalFormat("#,##0.0#", SIMBOLOS_ES);
        String signo = conSigno && valor.signum() > 0 ? "+" : "";
        return signo + formato.format(valor) + " %";
    }
}
