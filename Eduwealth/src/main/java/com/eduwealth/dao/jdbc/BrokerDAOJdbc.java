package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BrokerDAOJdbc implements BrokerDAO {

    @Override
    public List<Broker> listarTodos() {
        String sql = "SELECT id_broker, nombre, descripcion, comisiones_info, comision_minima, tipos_activo, "
                + "nivel_recomendado, regulacion, url_referido FROM brokers ORDER BY nombre";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Broker> brokers = new ArrayList<>();
            while (rs.next()) {
                brokers.add(mapear(rs));
            }
            return brokers;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los brokers.", e);
        }
    }

    private Broker mapear(ResultSet rs) throws SQLException {
        return new Broker(
                rs.getInt("id_broker"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("comisiones_info"),
                rs.getBigDecimal("comision_minima"),
                convertirTipos(rs.getString("tipos_activo")),
                Nivel.valueOf(rs.getString("nivel_recomendado")),
                rs.getString("regulacion"),
                rs.getString("url_referido"));
    }

    /** Convierte el valor de una columna SET de MySQL ("ACCION,ETF") en un conjunto de enumerados. */
    private Set<TipoActivo> convertirTipos(String valor) {
        Set<TipoActivo> tipos = EnumSet.noneOf(TipoActivo.class);
        if (valor != null && !valor.isBlank()) {
            for (String tipo : valor.split(",")) {
                tipos.add(TipoActivo.valueOf(tipo.trim()));
            }
        }
        return tipos;
    }
}
