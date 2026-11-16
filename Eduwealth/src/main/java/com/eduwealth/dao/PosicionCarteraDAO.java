package com.eduwealth.dao;

import com.eduwealth.model.PosicionCartera;

import java.util.List;

/** Acceso a datos de la tabla {@code posiciones_cartera}. */
public interface PosicionCarteraDAO {

    List<PosicionCartera> listarPorUsuario(int idUsuario);
}
