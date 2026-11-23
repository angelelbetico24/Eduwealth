package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.GastoDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Gasto;
import com.eduwealth.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GastoDAOJdbc implements GastoDAO {

    @Override
    public int insertar(Gasto gasto) {
        String sql = "INSERT INTO gastos (id_usuario, id_categoria, monto, fecha, concepto) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, gasto.getIdUsuario());
            ps.setInt(2, gasto.getIdCategoria());
            ps.setBigDecimal(3, gasto.getMonto());
            ps.setDate(4, Date.valueOf(gasto.getFecha()));
            ps.setString(5, gasto.getConcepto()); // puede ser null
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del nuevo gasto.");
                }
                gasto.setIdGasto(claves.getInt(1));
                return gasto.getIdGasto();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el gasto.", e);
        }
    }

    @Override
    public List<Gasto> listarDelMes(int idUsuario, int mes, int anio) {
        // Rango de fechas [día 1, día 1 del mes siguiente): aprovecha el índice (id_usuario, fecha)
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        String sql = "SELECT id_gasto, id_usuario, id_categoria, monto, fecha, concepto FROM gastos "
                + "WHERE id_usuario = ? AND fecha >= ? AND fecha < ? ORDER BY fecha DESC, id_gasto DESC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(inicio.plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) {
                List<Gasto> gastos = new ArrayList<>();
                while (rs.next()) {
                    gastos.add(new Gasto(
                            rs.getInt("id_gasto"),
                            rs.getInt("id_usuario"),
                            rs.getInt("id_categoria"),
                            rs.getBigDecimal("monto"),
                            rs.getDate("fecha").toLocalDate(),
                            rs.getString("concepto")));
                }
                return gastos;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar los gastos.", e);
        }
    }

    @Override
    public boolean eliminar(int idGasto, int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM gastos WHERE id_gasto = ? AND id_usuario = ?")) {
            ps.setInt(1, idGasto);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar el gasto.", e);
        }
    }

    @Override
    public void convertirImportes(Transaccion tx, int idUsuario, BigDecimal factor) {
        // Mínimo 0,01: un importe muy pequeño no debe redondearse a 0 (lo impediría el CHECK > 0)
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE gastos SET monto = GREATEST(ROUND(monto * ?, 2), 0.01) WHERE id_usuario = ?")) {
            ps.setBigDecimal(1, factor);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al convertir los importes a la nueva divisa.", e);
        }
    }
}
