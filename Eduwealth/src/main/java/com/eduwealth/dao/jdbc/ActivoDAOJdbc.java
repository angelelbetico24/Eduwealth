package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Activo;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivoDAOJdbc implements ActivoDAO {

    private static final String SELECT_BASE =
            "SELECT id_activo, ticker, nombre, tipo_activo, precio_actual, id_divisa FROM activos";

    @Override
    public List<Activo> listarTodos() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " ORDER BY tipo_activo, ticker");
             ResultSet rs = ps.executeQuery()) {
            List<Activo> activos = new ArrayList<>();
            while (rs.next()) {
                activos.add(mapear(rs));
            }
            return activos;
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

    private Activo mapear(ResultSet rs) throws SQLException {
        return new Activo(
                rs.getInt("id_activo"),
                rs.getString("ticker"),
                rs.getString("nombre"),
                TipoActivo.valueOf(rs.getString("tipo_activo")),
                rs.getBigDecimal("precio_actual"),
                rs.getInt("id_divisa"));
    }
}
