package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PosicionCarteraDAOJdbc implements PosicionCarteraDAO {

    private static final String SELECT_BASE =
            "SELECT id_posicion, id_usuario, id_activo, cantidad_titulos, precio_medio_compra FROM posiciones_cartera";

    @Override
    public List<PosicionCartera> listarPorUsuario(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " WHERE id_usuario = ? ORDER BY id_posicion")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<PosicionCartera> posiciones = new ArrayList<>();
                while (rs.next()) {
                    posiciones.add(mapear(rs));
                }
                return posiciones;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar la cartera.", e);
        }
    }

    @Override
    public Optional<PosicionCartera> bloquear(Transaccion tx, int idUsuario, int idActivo) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                SELECT_BASE + " WHERE id_usuario = ? AND id_activo = ? FOR UPDATE")) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idActivo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar la posición.", e);
        }
    }

    @Override
    public void insertar(Transaccion tx, PosicionCartera posicion) {
        Connection con = TransaccionJdbc.conexion(tx);
        String sql = "INSERT INTO posiciones_cartera (id_usuario, id_activo, cantidad_titulos, precio_medio_compra) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, posicion.getIdUsuario());
            ps.setInt(2, posicion.getIdActivo());
            ps.setBigDecimal(3, posicion.getCantidadTitulos());
            ps.setBigDecimal(4, posicion.getPrecioMedioCompra());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    posicion.setIdPosicion(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al añadir la posición a la cartera.", e);
        }
    }

    @Override
    public void actualizar(Transaccion tx, PosicionCartera posicion) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE posiciones_cartera SET cantidad_titulos = ?, precio_medio_compra = ? WHERE id_posicion = ?")) {
            ps.setBigDecimal(1, posicion.getCantidadTitulos());
            ps.setBigDecimal(2, posicion.getPrecioMedioCompra());
            ps.setInt(3, posicion.getIdPosicion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar la posición.", e);
        }
    }

    @Override
    public void eliminar(Transaccion tx, int idPosicion) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM posiciones_cartera WHERE id_posicion = ?")) {
            ps.setInt(1, idPosicion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al cerrar la posición.", e);
        }
    }

    private PosicionCartera mapear(ResultSet rs) throws SQLException {
        return new PosicionCartera(
                rs.getInt("id_posicion"),
                rs.getInt("id_usuario"),
                rs.getInt("id_activo"),
                rs.getBigDecimal("cantidad_titulos"),
                rs.getBigDecimal("precio_medio_compra"));
    }

    @Override
    public void eliminarTodas(Transaccion tx, int idUsuario) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM posiciones_cartera WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al vaciar la cartera.", e);
        }
    }
}
