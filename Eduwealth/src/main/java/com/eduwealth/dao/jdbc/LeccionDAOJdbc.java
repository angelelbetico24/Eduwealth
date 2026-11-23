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
import java.sql.Statement;
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

    @Override
    public int insertar(Leccion l) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO lecciones (titulo, contenido, nivel) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, l.getTitulo());
            ps.setString(2, l.getContenido());
            ps.setString(3, l.getNivel().name());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                l.setIdLeccion(claves.getInt(1));
                return l.getIdLeccion();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar la lección.", e);
        }
    }

    @Override
    public void actualizar(Leccion l) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE lecciones SET titulo = ?, contenido = ?, nivel = ? WHERE id_leccion = ?")) {
            ps.setString(1, l.getTitulo());
            ps.setString(2, l.getContenido());
            ps.setString(3, l.getNivel().name());
            ps.setInt(4, l.getIdLeccion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar la lección.", e);
        }
    }

    @Override
    public boolean eliminar(int idLeccion) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM lecciones WHERE id_leccion = ?")) {
            ps.setInt(1, idLeccion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error al eliminar la lección.", e);
        }
    }
}
