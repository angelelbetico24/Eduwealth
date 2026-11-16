package com.eduwealth.service;

import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Rol;
import com.eduwealth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionServiceTest {

    private static final Divisa EUR = new Divisa(1, "EUR", "Euro", "€", BigDecimal.ONE, null);

    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AutenticacionService(new UsuarioDAOEnMemoria());
    }

    @Test
    void registrarAsignaSaldoInicialRolYHashBcrypt() {
        Usuario u = servicio.registrar("Ana", "  Ana@Correo.COM ", "secreta123", "secreta123", EUR);

        assertEquals("ana@correo.com", u.getEmail());
        assertEquals(Rol.USUARIO, u.getRol());
        assertEquals(0, new BigDecimal("10000.00").compareTo(u.getSaldoFicticio()));
        assertNotEquals("secreta123", u.getPasswordHash());
        assertTrue(u.getPasswordHash().startsWith("$2a$"));
    }

    @Test
    void registrarRechazaEmailDuplicado() {
        servicio.registrar("Ana", "ana@correo.com", "secreta123", "secreta123", EUR);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrar("Otra", "ANA@correo.com", "secreta123", "secreta123", EUR));
        assertEquals("email", e.getCampo());
    }

    @Test
    void registrarValidaCampos() {
        assertEquals("nombre", campoConError(" ", "a@b.com", "secreta123", "secreta123", EUR));
        assertEquals("email", campoConError("Ana", "no-es-un-correo", "secreta123", "secreta123", EUR));
        assertEquals("password", campoConError("Ana", "a@b.com", "corta", "corta", EUR));
        assertEquals("confirmacion", campoConError("Ana", "a@b.com", "secreta123", "distinta123", EUR));
        assertEquals("divisa", campoConError("Ana", "a@b.com", "secreta123", "secreta123", null));
    }

    @Test
    void iniciarSesionConCredencialesCorrectas() {
        servicio.registrar("Ana", "ana@correo.com", "secreta123", "secreta123", EUR);

        Usuario u = servicio.iniciarSesion("ANA@correo.com", "secreta123");
        assertEquals("Ana", u.getNombre());
    }

    @Test
    void iniciarSesionNoRevelaSiElCorreoExiste() {
        servicio.registrar("Ana", "ana@correo.com", "secreta123", "secreta123", EUR);

        ValidacionException passwordMal = assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion("ana@correo.com", "incorrecta"));
        ValidacionException correoInexistente = assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion("nadie@correo.com", "secreta123"));
        assertEquals(passwordMal.getMessage(), correoInexistente.getMessage());
    }

    private String campoConError(String nombre, String email, String pass, String conf, Divisa divisa) {
        return assertThrows(ValidacionException.class,
                () -> servicio.registrar(nombre, email, pass, conf, divisa)).getCampo();
    }

    /** DAO falso en memoria: permite probar el servicio sin base de datos. */
    private static final class UsuarioDAOEnMemoria implements UsuarioDAO {

        private final Map<String, Usuario> porEmail = new HashMap<>();
        private int siguienteId = 1;

        @Override
        public Optional<Usuario> buscarPorId(int idUsuario) {
            return porEmail.values().stream().filter(u -> u.getIdUsuario() == idUsuario).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return Optional.ofNullable(porEmail.get(email));
        }

        @Override
        public boolean existeEmail(String email) {
            return porEmail.containsKey(email);
        }

        @Override
        public int insertar(Usuario usuario) {
            usuario.setIdUsuario(siguienteId++);
            porEmail.put(usuario.getEmail(), usuario);
            return usuario.getIdUsuario();
        }
    }
}
