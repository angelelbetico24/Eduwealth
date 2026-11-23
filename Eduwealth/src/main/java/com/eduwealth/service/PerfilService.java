package com.eduwealth.service;

import com.eduwealth.dao.GastoDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.dao.PresupuestoDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Configuración del perfil (RF-03): nombre, contraseña y divisa base; y reinicio del simulador
 * (RF-19, CU-13). Las operaciones que tocan varias tablas van en una transacción.
 */
public class PerfilService {

    private static final int LONGITUD_MAXIMA_NOMBRE = 100;

    private final UsuarioDAO usuarioDAO;
    private final PosicionCarteraDAO posicionDAO;
    private final OperacionSimuladaDAO operacionDAO;
    private final PresupuestoDAO presupuestoDAO;
    private final GastoDAO gastoDAO;
    private final DivisaService divisaService;
    private final GestorTransacciones transacciones;

    public PerfilService(UsuarioDAO usuarioDAO, PosicionCarteraDAO posicionDAO, OperacionSimuladaDAO operacionDAO,
                         PresupuestoDAO presupuestoDAO, GastoDAO gastoDAO, DivisaService divisaService,
                         GestorTransacciones transacciones) {
        this.usuarioDAO = usuarioDAO;
        this.posicionDAO = posicionDAO;
        this.operacionDAO = operacionDAO;
        this.presupuestoDAO = presupuestoDAO;
        this.gastoDAO = gastoDAO;
        this.divisaService = divisaService;
        this.transacciones = transacciones;
    }

    /** Cambia el nombre visible del usuario y devuelve el nombre guardado. */
    public String cambiarNombre(int idUsuario, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("nombre", "Introduce tu nombre.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new ValidacionException("nombre", "El nombre no puede superar los 100 caracteres.");
        }
        usuarioDAO.actualizarNombre(idUsuario, limpio);
        return limpio;
    }

    /** RF-03: cambia la contraseña comprobando antes la actual (RNF-01: se guarda con BCrypt). */
    public void cambiarPassword(int idUsuario, String actual, String nueva, String confirmacion) {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new ValidacionException("El usuario no existe."));
        if (actual == null || actual.isEmpty() || !BCrypt.checkpw(actual, usuario.getPasswordHash())) {
            throw new ValidacionException("actual", "La contraseña actual no es correcta.");
        }
        if (nueva == null || nueva.length() < AutenticacionService.LONGITUD_MINIMA_PASSWORD) {
            throw new ValidacionException("nueva", "La nueva contraseña debe tener al menos "
                    + AutenticacionService.LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }
        if (nueva.equals(actual)) {
            throw new ValidacionException("nueva", "La nueva contraseña debe ser distinta de la actual.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ValidacionException("confirmacion", "Las contraseñas no coinciden.");
        }
        usuarioDAO.actualizarPassword(idUsuario, BCrypt.hashpw(nueva, BCrypt.gensalt()));
    }

    /**
     * RF-03 + RN-04 (C-07, C-28): cambia la divisa base convirtiendo al tipo de cambio actual el saldo,
     * el capital inicial, los límites de presupuesto y los gastos, todo en una transacción.
     * Las posiciones no cambian: están en la divisa de cada activo.
     *
     * @return el usuario con los valores actualizados
     */
    public Usuario cambiarDivisaBase(int idUsuario, Divisa nueva) {
        if (nueva == null) {
            throw new ValidacionException("divisa", "Selecciona una divisa.");
        }
        try (Transaccion tx = transacciones.iniciar()) {
            Usuario usuario = usuarioDAO.bloquearPorId(tx, idUsuario)
                    .orElseThrow(() -> new ValidacionException("El usuario no existe."));
            if (usuario.getIdDivisaBase() == nueva.getIdDivisa()) {
                return usuario; // nada que convertir
            }
            Divisa actual = divisaService.buscarPorId(usuario.getIdDivisaBase())
                    .orElseThrow(() -> new ValidacionException("Tu divisa actual no está configurada."));

            BigDecimal factor = divisaService.convertir(BigDecimal.ONE, actual, nueva)
                    .round(MathContext.DECIMAL64);
            BigDecimal saldo = aCentimos(divisaService.convertir(usuario.getSaldoFicticio(), actual, nueva));
            BigDecimal capital = aCentimos(divisaService.convertir(usuario.getCapitalInicial(), actual, nueva));

            usuarioDAO.actualizarCuenta(tx, idUsuario, nueva.getIdDivisa(), saldo, capital);
            presupuestoDAO.convertirImportes(tx, idUsuario, factor);
            gastoDAO.convertirImportes(tx, idUsuario, factor);
            tx.confirmar();

            usuario.setIdDivisaBase(nueva.getIdDivisa());
            usuario.setSaldoFicticio(saldo);
            usuario.setCapitalInicial(capital);
            return usuario;
        }
    }

    /**
     * RF-19 / CU-13 (C-14): borra posiciones e historial del simulador y restablece saldo y capital
     * inicial a 10.000 en la divisa base. No toca el progreso educativo ni los presupuestos.
     *
     * @return el nuevo saldo
     */
    public BigDecimal reiniciarSimulador(int idUsuario) {
        try (Transaccion tx = transacciones.iniciar()) {
            Usuario usuario = usuarioDAO.bloquearPorId(tx, idUsuario)
                    .orElseThrow(() -> new ValidacionException("El usuario no existe."));
            posicionDAO.eliminarTodas(tx, idUsuario);
            operacionDAO.eliminarTodas(tx, idUsuario);
            BigDecimal saldo = AutenticacionService.SALDO_INICIAL;
            usuarioDAO.actualizarCuenta(tx, idUsuario, usuario.getIdDivisaBase(), saldo, saldo);
            tx.confirmar();
            return saldo;
        }
    }

    private static BigDecimal aCentimos(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP);
    }
}
