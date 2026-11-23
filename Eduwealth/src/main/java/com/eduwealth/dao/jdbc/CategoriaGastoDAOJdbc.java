package com.eduwealth.dao.jdbc;

import com.eduwealth.dao.CategoriaGastoDAO;
import com.eduwealth.dao.DataAccessException;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaGastoDAOJdbc implements CategoriaGastoDAO {

    @Override
    public List<CategoriaGasto> listarTodas() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_categoria, nombre, descripcion FROM categorias_gastos ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            List<CategoriaGasto> categorias = new ArrayList<>();
            while (rs.next()) {
                categorias.add(new CategoriaGasto(
                        rs.getInt("id_categoria"), rs.getString("nombre"), rs.getString("descripcion")));
            }
            return categorias;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar las categorías de gasto.", e);
        }
    }
}
