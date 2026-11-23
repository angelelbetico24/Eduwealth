package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.PreguntaDAO;
import com.eduwealth.model.Pregunta;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    @Override
    public int insertar(Pregunta p) {
        String sql = "INSERT INTO preguntas (id_leccion, enunciado, opcion_a, opcion_b, opcion_c, opcion_d, "
                + "respuesta_correcta) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdLeccion());
            rellenar(ps, p, 2);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                p.setIdPregunta(claves.getInt(1));
                return p.getIdPregunta();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar la pregunta.", e);
        }
    }

    @Override
    public void actualizar(Pregunta p) {
        String sql = "UPDATE preguntas SET enunciado = ?, opcion_a = ?, opcion_b = ?, opcion_c = ?, opcion_d = ?, "
                + "respuesta_correcta = ? WHERE id_pregunta = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            rellenar(ps, p, 1);
            ps.setInt(7, p.getIdPregunta());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar la pregunta.", e);
        }
    }

    @Override
    public boolean eliminar(int idPregunta) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM preguntas WHERE id_pregunta = ?")) {
            ps.setInt(1, idPregunta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar la pregunta.", e);
        }
    }

    /** Enunciado, opciones A-D y respuesta correcta a partir del parámetro {@code desde}. */
    private void rellenar(PreparedStatement ps, Pregunta p, int desde) throws SQLException {
        ps.setString(desde, p.getEnunciado());
        ps.setString(desde + 1, p.getOpcionA());
        ps.setString(desde + 2, p.getOpcionB());
        ps.setString(desde + 3, p.getOpcionC());
        ps.setString(desde + 4, p.getOpcionD());
        ps.setString(desde + 5, String.valueOf(p.getRespuestaCorrecta()));
    }
}
