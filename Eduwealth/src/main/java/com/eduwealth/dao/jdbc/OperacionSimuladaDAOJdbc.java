package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OperacionSimuladaDAOJdbc implements OperacionSimuladaDAO {

    @Override
    public int contarPorUsuario(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM operaciones_simuladas WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al contar las operaciones.", e);
        }
    }
}
