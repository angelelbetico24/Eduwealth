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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public int insertar(Broker b) {
        String sql = "INSERT INTO brokers (nombre, descripcion, comisiones_info, comision_minima, tipos_activo, "
                + "nivel_recomendado, regulacion, url_referido) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            rellenar(ps, b);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                b.setIdBroker(claves.getInt(1));
                return b.getIdBroker();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar el broker.", e);
        }
    }

    @Override
    public void actualizar(Broker b) {
        String sql = "UPDATE brokers SET nombre = ?, descripcion = ?, comisiones_info = ?, comision_minima = ?, "
                + "tipos_activo = ?, nivel_recomendado = ?, regulacion = ?, url_referido = ? WHERE id_broker = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            rellenar(ps, b);
            ps.setInt(9, b.getIdBroker());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar el broker.", e);
        }
    }

    @Override
    public boolean eliminar(int idBroker) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM brokers WHERE id_broker = ?")) {
            ps.setInt(1, idBroker);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar el broker.", e);
        }
    }

    private void rellenar(PreparedStatement ps, Broker b) throws SQLException {
        ps.setString(1, b.getNombre());
        ps.setString(2, b.getDescripcion());
        ps.setString(3, b.getComisionesInfo());
        ps.setBigDecimal(4, b.getComisionMinima());
        // Columna SET de MySQL: valores separados por comas, por ejemplo ACCION,ETF
        ps.setString(5, b.getTiposActivo().stream().map(Enum::name).collect(Collectors.joining(",")));
        ps.setString(6, b.getNivelRecomendado().name());
        ps.setString(7, b.getRegulacion());
        ps.setString(8, b.getUrlReferido());
    }
}
