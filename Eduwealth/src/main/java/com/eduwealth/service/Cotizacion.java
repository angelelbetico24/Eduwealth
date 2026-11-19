package com.eduwealth.service;

import java.math.BigDecimal;

/**
 * Vista previa de una orden antes de confirmarla (CU-04 paso 4, FA-01).
 *
 * @param importeActivo importe en la divisa del activo
 * @param importeBase   importe convertido a la divisa base y redondeado a céntimos
 * @param permitida     si la orden cumple RN-01 / RN-02
 * @param motivo        explicación cuando no está permitida (null si lo está)
 */
public record Cotizacion(BigDecimal importeActivo, BigDecimal importeBase, boolean permitida, String motivo) {
}
