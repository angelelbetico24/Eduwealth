package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.ProgresoLeccionDAO;
import com.eduwealth.model.ProgresoLeccion;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProgresoLeccionDAOJdbc implements ProgresoLeccionDAO {

    @Override
    public List<ProgresoLeccion> listarPorUsuario(int idUsuario) {
        String sql = "SELECT id_progreso, id_usuario, id_leccion, completada, nota_test, fecha_completado "
                + "FROM progresos_lecciones WHERE id_usuario = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProgresoLeccion> progresos = new ArrayList<>();
                while (rs.next()) {
                    Timestamp fecha = rs.getTimestamp("fecha_completado");
                    progresos.add(new ProgresoLeccion(
                            rs.getInt("id_progreso"),
                            rs.getInt("id_usuario"),
                            rs.getInt("id_leccion"),
                            rs.getBoolean("completada"),
                            rs.getBigDecimal("nota_test"),
                            fecha != null ? fecha.toLocalDateTime() : null));
                }
                return progresos;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el progreso educativo.", e);
        }
    }
}
