package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.model.Divisa;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DivisaDAOJdbc implements DivisaDAO {

    private static final String SELECT_BASE =
            "SELECT id_divisa, codigo, nombre, simbolo, tipo_cambio_base, fecha_actualizacion FROM divisas";

    @Override
    public List<Divisa> listarTodas() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " ORDER BY id_divisa");
             ResultSet rs = ps.executeQuery()) {
            List<Divisa> divisas = new ArrayList<>();
            while (rs.next()) {
                divisas.add(mapear(rs));
            }
            return divisas;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar las divisas.", e);
        }
    }

    @Override
    public Optional<Divisa> buscarPorId(int idDivisa) {
        return buscarUna(SELECT_BASE + " WHERE id_divisa = ?", idDivisa);
    }

    @Override
    public Optional<Divisa> buscarPorCodigo(String codigo) {
        return buscarUna(SELECT_BASE + " WHERE codigo = ?", codigo);
    }

    private Optional<Divisa> buscarUna(String sql, Object parametro) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar la divisa.", e);
        }
    }

    private Divisa mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("fecha_actualizacion");
        return new Divisa(
                rs.getInt("id_divisa"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("simbolo"),
                rs.getBigDecimal("tipo_cambio_base"),
                fecha != null ? fecha.toLocalDateTime() : null);
    }
}
