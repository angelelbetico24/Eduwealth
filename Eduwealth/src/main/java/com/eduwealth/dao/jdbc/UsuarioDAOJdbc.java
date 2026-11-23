package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.RegistroDuplicadoException;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Rol;
import com.eduwealth.model.Usuario;
import com.eduwealth.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class UsuarioDAOJdbc implements UsuarioDAO {

    private static final String SELECT_BASE =
            "SELECT id_usuario, nombre, email, password_hash, rol, saldo_ficticio, capital_inicial, id_divisa_base, "
                    + "fecha_registro FROM usuarios";

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return buscarUno(SELECT_BASE + " WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return buscarUno(SELECT_BASE + " WHERE email = ?", email);
    }

    @Override
    public boolean existeEmail(String email) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM usuarios WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al comprobar el correo electrónico.", e);
        }
    }

    @Override
    public int insertar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, email, password_hash, rol, saldo_ficticio, capital_inicial, "
                + "id_divisa_base) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getPasswordHash());
            ps.setString(4, usuario.getRol().name());
            ps.setBigDecimal(5, usuario.getSaldoFicticio());
            ps.setBigDecimal(6, usuario.getCapitalInicial());
            ps.setInt(7, usuario.getIdDivisaBase());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del nuevo usuario.");
                }
                int id = claves.getInt(1);
                usuario.setIdUsuario(id);
                return id;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Índice UNIQUE de email (caso límite 6.9.6): cubre también el registro simultáneo.
            throw new RegistroDuplicadoException("Ya existe un usuario con ese correo electrónico.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el usuario.", e);
        }
    }

    private Optional<Usuario> buscarUno(String sql, Object parametro) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el usuario.", e);
        }
    }

    @Override
    public Optional<Usuario> bloquearPorId(Transaccion tx, int idUsuario) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(SELECT_BASE + " WHERE id_usuario = ? FOR UPDATE")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al leer el saldo del usuario.", e);
        }
    }

    @Override
    public void actualizarSaldo(Transaccion tx, int idUsuario, BigDecimal saldo) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE usuarios SET saldo_ficticio = ? WHERE id_usuario = ?")) {
            ps.setBigDecimal(1, saldo);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el saldo.", e);
        }
    }

    @Override
    public void actualizarNombre(int idUsuario, String nombre) {
        ejecutarActualizacion("UPDATE usuarios SET nombre = ? WHERE id_usuario = ?", nombre, idUsuario,
                "Error al guardar el nombre.");
    }

    @Override
    public void actualizarPassword(int idUsuario, String passwordHash) {
        ejecutarActualizacion("UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?", passwordHash, idUsuario,
                "Error al cambiar la contraseña.");
    }

    @Override
    public void actualizarCuenta(Transaccion tx, int idUsuario, int idDivisaBase, BigDecimal saldo,
                                 BigDecimal capitalInicial) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET id_divisa_base = ?, saldo_ficticio = ?, "
                + "capital_inicial = ? WHERE id_usuario = ?")) {
            ps.setInt(1, idDivisaBase);
            ps.setBigDecimal(2, saldo);
            ps.setBigDecimal(3, capitalInicial);
            ps.setInt(4, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar la cuenta del simulador.", e);
        }
    }

    private void ejecutarActualizacion(String sql, String valor, int idUsuario, String mensajeError) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(mensajeError, e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Timestamp registro = rs.getTimestamp("fecha_registro");
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Rol.valueOf(rs.getString("rol")),
                rs.getBigDecimal("saldo_ficticio"),
                rs.getBigDecimal("capital_inicial"),
                rs.getInt("id_divisa_base"),
                registro != null ? registro.toLocalDateTime() : null);
    }
}
