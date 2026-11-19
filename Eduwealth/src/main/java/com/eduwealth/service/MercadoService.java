package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Activo;
import com.eduwealth.model.TipoActivo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.random.RandomGenerator;

/**
 * Movimiento simulado del mercado (C-26). Los precios del catálogo cambian una vez por día
 * transcurrido con una variación aleatoria acorde al tipo de activo, sin conexión externa.
 * <p>
 * Modelo: cada día el precio se multiplica por {@code 1 + r}, con {@code r} normal de media
 * {@code tendencia} y desviación {@code volatilidad}, limitado a ±25 % para evitar saltos absurdos.
 */
public class MercadoService {

    /** Días máximos que se simulan de una vez (si nadie entra en meses no se recalcula un año entero). */
    static final int MAX_DIAS_POR_ACTUALIZACION = 30;
    private static final double LIMITE_DIARIO = 0.25;
    private static final BigDecimal PRECIO_MINIMO = new BigDecimal("0.0001");

    private final ActivoDAO activoDAO;
    private final GestorTransacciones transacciones;
    private final RandomGenerator azar;

    public MercadoService(ActivoDAO activoDAO, GestorTransacciones transacciones, RandomGenerator azar) {
        this.activoDAO = activoDAO;
        this.transacciones = transacciones;
        this.azar = azar;
    }

    /**
     * Aplica a cada activo un movimiento por cada día natural transcurrido desde su última
     * actualización. Se llama al iniciar sesión. Es idempotente dentro del mismo día: si ya se
     * actualizó hoy, no cambia nada. Devuelve cuántos activos han cambiado de precio.
     */
    public int actualizarPrecios(LocalDateTime ahora) {
        try (Transaccion tx = transacciones.iniciar()) {
            int actualizados = 0;
            // Bloqueo de los activos: si dos usuarios entran a la vez, solo uno aplica el movimiento
            for (Activo activo : activoDAO.bloquearTodos(tx)) {
                long dias = diasPendientes(activo, ahora);
                if (dias > 0) {
                    BigDecimal nuevo = simularDias(activo.getPrecioActual(), activo.getTipoActivo(), dias);
                    activoDAO.actualizarPrecio(tx, activo.getIdActivo(), nuevo, ahora);
                    actualizados++;
                }
            }
            tx.confirmar();
            return actualizados;
        }
    }

    /**
     * Simula un día de mercado para todos los activos, sin mirar la fecha. Pensado para el
     * administrador (demostraciones, CU-12), no para el usuario: así nadie puede forzar el
     * mercado hasta obtener beneficio.
     */
    public void simularUnDia(LocalDateTime ahora) {
        try (Transaccion tx = transacciones.iniciar()) {
            for (Activo activo : activoDAO.bloquearTodos(tx)) {
                activoDAO.actualizarPrecio(tx, activo.getIdActivo(),
                        simularDias(activo.getPrecioActual(), activo.getTipoActivo(), 1), ahora);
            }
            tx.confirmar();
        }
    }

    /** Días naturales desde la última actualización, limitados a {@link #MAX_DIAS_POR_ACTUALIZACION}. */
    long diasPendientes(Activo activo, LocalDateTime ahora) {
        if (activo.getFechaActualizacion() == null) {
            return 1;
        }
        long dias = ChronoUnit.DAYS.between(activo.getFechaActualizacion().toLocalDate(), ahora.toLocalDate());
        return Math.max(0, Math.min(dias, MAX_DIAS_POR_ACTUALIZACION));
    }

    /** Aplica {@code dias} variaciones diarias al precio. Resultado con 4 decimales y siempre positivo. */
    BigDecimal simularDias(BigDecimal precio, TipoActivo tipo, long dias) {
        Perfil perfil = Perfil.de(tipo);
        double valor = precio.doubleValue();
        for (long d = 0; d < dias; d++) {
            double r = perfil.tendencia + perfil.volatilidad * azar.nextGaussian();
            r = Math.max(-LIMITE_DIARIO, Math.min(LIMITE_DIARIO, r));
            valor *= 1 + r;
        }
        BigDecimal resultado = BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP);
        return resultado.max(PRECIO_MINIMO);
    }

    /**
     * Volatilidad y tendencia diarias por tipo de activo. La tendencia positiva de acciones y
     * fondos (≈ 7 % anual) refleja que, a largo plazo, la renta variable suele crecer.
     */
    enum Perfil {
        ACCION(0.02, 0.0003),
        ETF(0.01, 0.0003),
        FONDO(0.008, 0.0002),
        CRIPTO(0.05, 0.0);

        final double volatilidad;
        final double tendencia;

        Perfil(double volatilidad, double tendencia) {
            this.volatilidad = volatilidad;
            this.tendencia = tendencia;
        }

        static Perfil de(TipoActivo tipo) {
            return valueOf(tipo.name());
        }
    }
}
