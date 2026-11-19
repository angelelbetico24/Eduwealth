package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Activo;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivoDAOJdbc implements ActivoDAO {

    private static final String SELECT_BASE = "SELECT id_activo, ticker, nombre, tipo_activo, precio_actual, "
            + "id_divisa, fecha_actualizacion FROM activos";

    @Override
    public List<Activo> listarTodos() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " ORDER BY tipo_activo, ticker");
             ResultSet rs = ps.executeQuery()) {
            return mapearTodos(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los activos.", e);
        }
    }

    @Override
    public Optional<Activo> buscarPorId(int idActivo) {
        return buscarUno(SELECT_BASE + " WHERE id_activo = ?", idActivo);
    }

    @Override
    public Optional<Activo> buscarPorTicker(String ticker) {
        return buscarUno(SELECT_BASE + " WHERE ticker = ?", ticker);
    }

    @Override
    public List<Activo> bloquearTodos(Transaccion tx) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(SELECT_BASE + " ORDER BY id_activo FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {
            return mapearTodos(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Error al leer los precios de los activos.", e);
        }
    }

    @Override
    public void actualizarPrecio(Transaccion tx, int idActivo, BigDecimal precio, LocalDateTime fecha) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE activos SET precio_actual = ?, fecha_actualizacion = ? WHERE id_activo = ?")) {
            ps.setBigDecimal(1, precio);
            ps.setTimestamp(2, Timestamp.valueOf(fecha));
            ps.setInt(3, idActivo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el precio del activo.", e);
        }
    }

    private Optional<Activo> buscarUno(String sql, Object parametro) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el activo.", e);
        }
    }

    private List<Activo> mapearTodos(ResultSet rs) throws SQLException {
        List<Activo> activos = new ArrayList<>();
        while (rs.next()) {
            activos.add(mapear(rs));
        }
        return activos;
    }

    private Activo mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("fecha_actualizacion");
        return new Activo(
                rs.getInt("id_activo"),
                rs.getString("ticker"),
                rs.getString("nombre"),
                TipoActivo.valueOf(rs.getString("tipo_activo")),
                rs.getBigDecimal("precio_actual"),
                rs.getInt("id_divisa"),
                fecha != null ? fecha.toLocalDateTime() : null);
    }
}
