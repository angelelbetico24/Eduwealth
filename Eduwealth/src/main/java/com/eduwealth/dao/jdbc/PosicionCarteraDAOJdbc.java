package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PosicionCarteraDAOJdbc implements PosicionCarteraDAO {

    @Override
    public List<PosicionCartera> listarPorUsuario(int idUsuario) {
        String sql = "SELECT id_posicion, id_usuario, id_activo, cantidad_titulos, precio_medio_compra "
                + "FROM posiciones_cartera WHERE id_usuario = ? ORDER BY id_posicion";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<PosicionCartera> posiciones = new ArrayList<>();
                while (rs.next()) {
                    posiciones.add(new PosicionCartera(
                            rs.getInt("id_posicion"),
                            rs.getInt("id_usuario"),
                            rs.getInt("id_activo"),
                            rs.getBigDecimal("cantidad_titulos"),
                            rs.getBigDecimal("precio_medio_compra")));
                }
                return posiciones;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar la cartera.", e);
        }
    }
}
