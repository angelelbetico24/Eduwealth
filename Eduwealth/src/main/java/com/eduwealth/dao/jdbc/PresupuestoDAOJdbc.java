package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.PresupuestoDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Presupuesto;
import com.eduwealth.model.ResumenCategoria;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAOJdbc implements PresupuestoDAO {

    /**
     * Una sola consulta: cada presupuesto del mes con la suma de sus gastos del mismo mes
     * (LEFT JOIN para incluir categorías sin gastos todavía).
     */
    private static final String SQL_RESUMEN_MES = """
            SELECT p.id_categoria, c.nombre, p.monto_limite, COALESCE(SUM(g.monto), 0) AS gastado
            FROM presupuestos p
            JOIN categorias_gastos c ON c.id_categoria = p.id_categoria
            LEFT JOIN gastos g ON g.id_usuario = p.id_usuario
                              AND g.id_categoria = p.id_categoria
                              AND MONTH(g.fecha) = p.mes AND YEAR(g.fecha) = p.anio
            WHERE p.id_usuario = ? AND p.mes = ? AND p.anio = ?
            GROUP BY p.id_presupuesto, p.id_categoria, c.nombre, p.monto_limite
            ORDER BY c.nombre
            """;

    @Override
    public List<ResumenCategoria> listarResumenMes(int idUsuario, int mes, int anio) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_RESUMEN_MES)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, mes);
            ps.setInt(3, anio);
            try (ResultSet rs = ps.executeQuery()) {
                List<ResumenCategoria> resumen = new ArrayList<>();
                while (rs.next()) {
                    resumen.add(new ResumenCategoria(
                            rs.getInt("id_categoria"),
                            rs.getString("nombre"),
                            rs.getBigDecimal("monto_limite"),
                            rs.getBigDecimal("gastado")));
                }
                return resumen;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar los presupuestos.", e);
        }
    }

    @Override
    public void guardar(Transaccion tx, Presupuesto presupuesto) {
        // Una sola sentencia atómica: inserta o, si ya existe el UNIQUE (usuario, categoría, mes, año), actualiza
        String sql = "INSERT INTO presupuestos (id_usuario, id_categoria, monto_limite, mes, anio) "
                + "VALUES (?, ?, ?, ?, ?) AS nuevo ON DUPLICATE KEY UPDATE monto_limite = nuevo.monto_limite";
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, presupuesto.getIdUsuario());
            ps.setInt(2, presupuesto.getIdCategoria());
            ps.setBigDecimal(3, presupuesto.getMontoLimite());
            ps.setInt(4, presupuesto.getMes());
            ps.setInt(5, presupuesto.getAnio());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar el presupuesto.", e);
        }
    }

    @Override
    public boolean eliminar(Transaccion tx, int idUsuario, int idCategoria, int mes, int anio) {
        Connection con = TransaccionJdbc.conexion(tx);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM presupuestos WHERE id_usuario = ? AND id_categoria = ? AND mes = ? AND anio = ?")) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setInt(3, mes);
            ps.setInt(4, anio);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar el presupuesto.", e);
        }
    }
}
