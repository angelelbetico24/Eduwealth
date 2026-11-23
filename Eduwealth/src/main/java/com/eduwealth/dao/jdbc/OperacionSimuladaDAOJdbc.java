package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.TipoOperacion;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OperacionSimuladaDAOJdbc implements OperacionSimuladaDAO {

    @Override
    public int contarPorUsuario(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM operaciones_simuladas WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al contar las operaciones.", e);
        }
    }

    @Override
    public List<OperacionSimulada> listarPorUsuario(int idUsuario) {
        String sql = "SELECT id_operacion, id_usuario, id_activo, tipo_operacion, cantidad, precio_unitario, "
                + "pnl_realizado, fecha_hora FROM operaciones_simuladas WHERE id_usuario = ? "
                + "ORDER BY fecha_hora DESC, id_operacion DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<OperacionSimulada> operaciones = new ArrayList<>();
                while (rs.next()) {
                    Timestamp fecha = rs.getTimestamp("fecha_hora");
                    operaciones.add(new OperacionSimulada(
                            rs.getInt("id_operacion"),
                            rs.getInt("id_usuario"),
                            rs.getInt("id_activo"),
                            TipoOperacion.valueOf(rs.getString("tipo_operacion")),
                            rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("pnl_realizado"),
                            fecha != null ? fecha.toLocalDateTime() : null));
                }
                return operaciones;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el historial de operaciones.", e);
        }
    }

    @Override
    public void insertar(Transaccion tx, OperacionSimulada operacion) {
        Connection con = TransaccionJdbc.conexion(tx);
        String sql = "INSERT INTO operaciones_simuladas "
                + "(id_usuario, id_activo, tipo_operacion, cantidad, precio_unitario, pnl_realizado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, operacion.getIdUsuario());
            ps.setInt(2, operacion.getIdActivo());
            ps.setString(3, operacion.getTipoOperacion().name());
            ps.setBigDecimal(4, operacion.getCantidad());
            ps.setBigDecimal(5, operacion.getPrecioUnitario());
            ps.setBigDecimal(6, operacion.getPnlRealizado()); // null en las compras
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    operacion.setIdOperacion(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar la operación en el historial.", e);
        }
    }

    @Override
    public void eliminarTodas(Transaccion tx, int idUsuario) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM operaciones_simuladas WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al borrar el historial de operaciones.", e);
        }
    }
}
