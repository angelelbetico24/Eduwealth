package com.eduwealth.dao;

import java.math.BigDecimal;
import com.eduwealth.model.Gasto;

import java.util.List;

/** Acceso a datos de la tabla {@code gastos} (CU-11). */
public interface GastoDAO {

    /** Inserta el gasto y devuelve el identificador generado. */
    int insertar(Gasto gasto);

    /** Gastos del usuario en un mes, del más reciente al más antiguo. */
    List<Gasto> listarDelMes(int idUsuario, int mes, int anio);

    /** Elimina un gasto del usuario. Devuelve si existía (no se pueden borrar gastos de otro usuario). */
    boolean eliminar(int idGasto, int idUsuario);

    /** Multiplica todos los gastos del usuario por {@code factor} (cambio de divisa base, C-28). */
    void convertirImportes(Transaccion tx, int idUsuario, BigDecimal factor);
}
