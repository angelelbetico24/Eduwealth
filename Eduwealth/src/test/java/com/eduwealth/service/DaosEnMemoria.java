package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.model.Usuario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementaciones en memoria de los DAO para probar los servicios sin base de datos.
 */
final class DaosEnMemoria {

    private DaosEnMemoria() {
    }

    static final class Usuarios implements UsuarioDAO {
        final Map<Integer, Usuario> porId = new HashMap<>();
        private int siguienteId = 1;

        @Override
        public Optional<Usuario> buscarPorId(int idUsuario) {
            return Optional.ofNullable(porId.get(idUsuario));
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return porId.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existeEmail(String email) {
            return buscarPorEmail(email).isPresent();
        }

        @Override
        public int insertar(Usuario usuario) {
            usuario.setIdUsuario(siguienteId++);
            porId.put(usuario.getIdUsuario(), usuario);
            return usuario.getIdUsuario();
        }

        @Override
        public Optional<Usuario> bloquearPorId(Transaccion tx, int idUsuario) {
            return buscarPorId(idUsuario);
        }

        @Override
        public void actualizarSaldo(Transaccion tx, int idUsuario, BigDecimal saldo) {
            porId.get(idUsuario).setSaldoFicticio(saldo);
        }
    }

    static final class Divisas implements DivisaDAO {
        final Map<Integer, Divisa> porId = new HashMap<>();

        Divisas con(Divisa d) {
            porId.put(d.getIdDivisa(), d);
            return this;
        }

        @Override
        public List<Divisa> listarTodas() {
            return new ArrayList<>(porId.values());
        }

        @Override
        public Optional<Divisa> buscarPorId(int idDivisa) {
            return Optional.ofNullable(porId.get(idDivisa));
        }

        @Override
        public Optional<Divisa> buscarPorCodigo(String codigo) {
            return porId.values().stream().filter(d -> d.getCodigo().equals(codigo)).findFirst();
        }
    }

    static final class Activos implements ActivoDAO {
        final Map<Integer, Activo> porId = new HashMap<>();

        Activos con(Activo a) {
            porId.put(a.getIdActivo(), a);
            return this;
        }

        @Override
        public List<Activo> listarTodos() {
            return new ArrayList<>(porId.values());
        }

        @Override
        public Optional<Activo> buscarPorId(int idActivo) {
            return Optional.ofNullable(porId.get(idActivo));
        }

        @Override
        public Optional<Activo> buscarPorTicker(String ticker) {
            return porId.values().stream().filter(a -> a.getTicker().equals(ticker)).findFirst();
        }
    }

    static final class Posiciones implements PosicionCarteraDAO {
        final Map<Integer, PosicionCartera> porId = new HashMap<>();
        private int siguienteId = 1;

        @Override
        public List<PosicionCartera> listarPorUsuario(int idUsuario) {
            return porId.values().stream().filter(p -> p.getIdUsuario() == idUsuario).toList();
        }

        @Override
        public Optional<PosicionCartera> bloquear(Transaccion tx, int idUsuario, int idActivo) {
            return porId.values().stream()
                    .filter(p -> p.getIdUsuario() == idUsuario && p.getIdActivo() == idActivo)
                    .findFirst();
        }

        @Override
        public void insertar(Transaccion tx, PosicionCartera posicion) {
            posicion.setIdPosicion(siguienteId++);
            porId.put(posicion.getIdPosicion(), posicion);
        }

        @Override
        public void actualizar(Transaccion tx, PosicionCartera posicion) {
            porId.put(posicion.getIdPosicion(), posicion);
        }

        @Override
        public void eliminar(Transaccion tx, int idPosicion) {
            porId.remove(idPosicion);
        }
    }

    static final class Operaciones implements OperacionSimuladaDAO {
        final List<OperacionSimulada> todas = new ArrayList<>();

        @Override
        public int contarPorUsuario(int idUsuario) {
            return (int) todas.stream().filter(o -> o.getIdUsuario() == idUsuario).count();
        }

        @Override
        public List<OperacionSimulada> listarPorUsuario(int idUsuario) {
            return todas.stream().filter(o -> o.getIdUsuario() == idUsuario).toList();
        }

        @Override
        public void insertar(Transaccion tx, OperacionSimulada operacion) {
            operacion.setIdOperacion(todas.size() + 1);
            todas.add(operacion);
        }
    }

    /** Cuenta las transacciones confirmadas y las cerradas sin confirmar (rollback). */
    static final class Transacciones implements GestorTransacciones {
        int confirmadas;
        int deshechas;

        @Override
        public Transaccion iniciar() {
            return new Transaccion() {
                private boolean confirmada;

                @Override
                public void confirmar() {
                    confirmada = true;
                    confirmadas++;
                }

                @Override
                public void close() {
                    if (!confirmada) {
                        deshechas++;
                    }
                }
            };
        }
    }
}
