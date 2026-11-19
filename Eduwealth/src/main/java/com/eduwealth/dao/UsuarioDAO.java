package com.eduwealth.dao;

import com.eduwealth.model.Usuario;

import java.math.BigDecimal;
import java.util.Optional;

/** Acceso a datos de la tabla {@code usuarios}. */
public interface UsuarioDAO {

    Optional<Usuario> buscarPorId(int idUsuario);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existeEmail(String email);

    /** Inserta el usuario y devuelve el identificador generado. */
    int insertar(Usuario usuario);

    /**
     * Lee el usuario dentro de la transacción y bloquea su fila hasta que termine
     * (SELECT ... FOR UPDATE), para que el saldo no cambie mientras se opera con él.
     */
    Optional<Usuario> bloquearPorId(Transaccion tx, int idUsuario);

    void actualizarSaldo(Transaccion tx, int idUsuario, BigDecimal saldo);
}
