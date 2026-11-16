package com.eduwealth.service;

import com.eduwealth.dao.RegistroDuplicadoException;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Rol;
import com.eduwealth.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registro (CU-01, RF-01, RF-04) e inicio de sesión (CU-02, RF-02) de usuarios.
 * Las contraseñas se almacenan con BCrypt (RNF-01, C-20).
 */
public class AutenticacionService {

    /** Saldo inicial del simulador en la divisa base del usuario (RF-04, C-07). */
    public static final BigDecimal SALDO_INICIAL = new BigDecimal("10000.00");
    public static final int LONGITUD_MINIMA_PASSWORD = 8;

    private static final int LONGITUD_MAXIMA_TEXTO = 100;
    private static final Pattern PATRON_EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final String CREDENCIALES_INCORRECTAS = "Correo electrónico o contraseña incorrectos.";

    private final UsuarioDAO usuarioDAO;

    public AutenticacionService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public Usuario registrar(String nombre, String email, String password, String confirmacion, Divisa divisaBase) {
        String nombreLimpio = validarNombre(nombre);
        String emailNormalizado = validarEmail(email);
        validarPassword(password, confirmacion);
        if (divisaBase == null) {
            throw new ValidacionException("divisa", "Selecciona tu moneda principal de referencia.");
        }
        if (usuarioDAO.existeEmail(emailNormalizado)) {
            throw new ValidacionException("email", "Ya existe una cuenta con ese correo electrónico.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreLimpio);
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        usuario.setRol(Rol.USUARIO);
        usuario.setSaldoFicticio(SALDO_INICIAL);
        usuario.setIdDivisaBase(divisaBase.getIdDivisa());
        try {
            usuarioDAO.insertar(usuario);
        } catch (RegistroDuplicadoException e) {
            throw new ValidacionException("email", "Ya existe una cuenta con ese correo electrónico.");
        }
        return usuario;
    }

    public Usuario iniciarSesion(String email, String password) {
        if (estaVacio(email) || estaVacio(password)) {
            throw new ValidacionException("Introduce tu correo electrónico y tu contraseña.");
        }
        // Mismo mensaje si el correo no existe o la contraseña falla: no se revela qué cuentas existen.
        Usuario usuario = usuarioDAO.buscarPorEmail(normalizarEmail(email))
                .orElseThrow(() -> new ValidacionException(CREDENCIALES_INCORRECTAS));
        if (!BCrypt.checkpw(password, usuario.getPasswordHash())) {
            throw new ValidacionException(CREDENCIALES_INCORRECTAS);
        }
        return usuario;
    }

    private String validarNombre(String nombre) {
        if (estaVacio(nombre)) {
            throw new ValidacionException("nombre", "Introduce tu nombre.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LONGITUD_MAXIMA_TEXTO) {
            throw new ValidacionException("nombre", "El nombre no puede superar los 100 caracteres.");
        }
        return limpio;
    }

    private String validarEmail(String email) {
        if (estaVacio(email)) {
            throw new ValidacionException("email", "Introduce tu correo electrónico.");
        }
        String normalizado = normalizarEmail(email);
        if (normalizado.length() > LONGITUD_MAXIMA_TEXTO || !PATRON_EMAIL.matcher(normalizado).matches()) {
            throw new ValidacionException("email", "El correo electrónico no tiene un formato válido.");
        }
        return normalizado;
    }

    private void validarPassword(String password, String confirmacion) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new ValidacionException("password",
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }
        if (!password.equals(confirmacion)) {
            throw new ValidacionException("confirmacion", "Las contraseñas no coinciden.");
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
