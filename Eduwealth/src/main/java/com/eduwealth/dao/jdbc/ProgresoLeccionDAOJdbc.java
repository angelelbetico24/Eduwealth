package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.ProgresoLeccionDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.ProgresoLeccion;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgresoLeccionDAOJdbc implements ProgresoLeccionDAO {

    private static final String SELECT_BASE = "SELECT id_progreso, id_usuario, id_leccion, completada, nota_test, "
            + "fecha_completado FROM progresos_lecciones";

    @Override
    public List<ProgresoLeccion> listarPorUsuario(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProgresoLeccion> progresos = new ArrayList<>();
                while (rs.next()) {
                    progresos.add(mapear(rs));
                }
                return progresos;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el progreso educativo.", e);
        }
    }

    @Override
    public Optional<ProgresoLeccion> bloquear(Transaccion tx, int idUsuario, int idLeccion) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                SELECT_BASE + " WHERE id_usuario = ? AND id_leccion = ? FOR UPDATE")) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idLeccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el progreso de la lección.", e);
        }
    }

    @Override
    public void insertar(Transaccion tx, ProgresoLeccion p) {
        Connection con = TransaccionJdbc.conexion(tx);
        String sql = "INSERT INTO progresos_lecciones (id_usuario, id_leccion, completada, nota_test, fecha_completado) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdUsuario());
            ps.setInt(2, p.getIdLeccion());
            ps.setBoolean(3, p.isCompletada());
            ps.setBigDecimal(4, p.getNotaTest());
            ps.setTimestamp(5, p.getFechaCompletado() == null ? null : Timestamp.valueOf(p.getFechaCompletado()));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    p.setIdProgreso(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar el resultado del test.", e);
        }
    }

    @Override
    public void actualizar(Transaccion tx, ProgresoLeccion p) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement("UPDATE progresos_lecciones SET completada = ?, "
                + "nota_test = ?, fecha_completado = ? WHERE id_progreso = ?")) {
            ps.setBoolean(1, p.isCompletada());
            ps.setBigDecimal(2, p.getNotaTest());
            ps.setTimestamp(3, p.getFechaCompletado() == null ? null : Timestamp.valueOf(p.getFechaCompletado()));
            ps.setInt(4, p.getIdProgreso());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar el resultado del test.", e);
        }
    }

    private ProgresoLeccion mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("fecha_completado");
        return new ProgresoLeccion(
                rs.getInt("id_progreso"),
                rs.getInt("id_usuario"),
                rs.getInt("id_leccion"),
                rs.getBoolean("completada"),
                rs.getBigDecimal("nota_test"),
                fecha != null ? fecha.toLocalDateTime() : null);
    }
}
