package com.eduwealth.dao;

import com.eduwealth.model.CategoriaGasto;

import java.util.List;

/** Acceso a datos de la tabla {@code categorias_gastos} (categorías globales, C-09). */
public interface CategoriaGastoDAO {

    List<CategoriaGasto> listarTodas();
}
