package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.LeccionDAO;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LeccionDAOJdbc implements LeccionDAO {

    @Override
    public List<Leccion> listarTodas() {
        // ORDER BY sobre un ENUM ordena por su posición en la definición: PRINCIPIANTE, INTERMEDIO, AVANZADO
        String sql = "SELECT id_leccion, titulo, contenido, nivel FROM lecciones ORDER BY nivel, id_leccion";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Leccion> lecciones = new ArrayList<>();
            while (rs.next()) {
                lecciones.add(new Leccion(
                        rs.getInt("id_leccion"),
                        rs.getString("titulo"),
                        rs.getString("contenido"),
                        Nivel.valueOf(rs.getString("nivel"))));
            }
            return lecciones;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar las lecciones.", e);
        }
    }
}
