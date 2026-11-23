package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.PreguntaDAO;
import com.eduwealth.model.Pregunta;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreguntaDAOJdbc implements PreguntaDAO {

    @Override
    public List<Pregunta> listarPorLeccion(int idLeccion) {
        String sql = "SELECT id_pregunta, id_leccion, enunciado, opcion_a, opcion_b, opcion_c, opcion_d, "
                + "respuesta_correcta FROM preguntas WHERE id_leccion = ? ORDER BY id_pregunta";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idLeccion);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pregunta> preguntas = new ArrayList<>();
                while (rs.next()) {
                    preguntas.add(new Pregunta(
                            rs.getInt("id_pregunta"),
                            rs.getInt("id_leccion"),
                            rs.getString("enunciado"),
                            rs.getString("opcion_a"),
                            rs.getString("opcion_b"),
                            rs.getString("opcion_c"),
                            rs.getString("opcion_d"),
                            rs.getString("respuesta_correcta").charAt(0)));
                }
                return preguntas;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al cargar las preguntas del test.", e);
        }
    }

    @Override
    public Map<Integer, Integer> contarPorLeccion() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_leccion, COUNT(*) AS total FROM preguntas GROUP BY id_leccion");
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, Integer> totales = new HashMap<>();
            while (rs.next()) {
                totales.put(rs.getInt("id_leccion"), rs.getInt("total"));
            }
            return totales;
        } catch (SQLException e) {
            throw new DataAccessException("Error al contar las preguntas.", e);
        }
    }
}
