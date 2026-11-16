package com.eduwealth.dao;

import com.eduwealth.model.Usuario;

import java.util.Optional;

/** Acceso a datos de la tabla {@code usuarios}. */
public interface UsuarioDAO {

    Optional<Usuario> buscarPorId(int idUsuario);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existeEmail(String email);

    /** Inserta el usuario y devuelve el identificador generado. */
    int insertar(Usuario usuario);
}
