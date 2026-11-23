package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.CategoriaGastoDAO;
import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.dao.GastoDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.LeccionDAO;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.dao.PreguntaDAO;
import com.eduwealth.dao.PresupuestoDAO;
import com.eduwealth.dao.ProgresoLeccionDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Activo;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Gasto;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.Presupuesto;
import com.eduwealth.model.ProgresoLeccion;
import com.eduwealth.model.ResumenCategoria;
import com.eduwealth.model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

        @Override
        public void actualizarNombre(int idUsuario, String nombre) {
            porId.get(idUsuario).setNombre(nombre);
        }

        @Override
        public void actualizarPassword(int idUsuario, String passwordHash) {
            porId.get(idUsuario).setPasswordHash(passwordHash);
        }

        @Override
        public void actualizarCuenta(Transaccion tx, int idUsuario, int idDivisaBase, BigDecimal saldo,
                                     BigDecimal capitalInicial) {
            Usuario u = porId.get(idUsuario);
            u.setIdDivisaBase(idDivisaBase);
            u.setSaldoFicticio(saldo);
            u.setCapitalInicial(capitalInicial);
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

        @Override
        public List<Activo> bloquearTodos(Transaccion tx) {
            return listarTodos();
        }

        @Override
        public void actualizarPrecio(Transaccion tx, int idActivo, BigDecimal precio, LocalDateTime fecha) {
            Activo activo = porId.get(idActivo);
            activo.setPrecioActual(precio);
            activo.setFechaActualizacion(fecha);
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

        @Override
        public void eliminarTodas(Transaccion tx, int idUsuario) {
            porId.values().removeIf(p -> p.getIdUsuario() == idUsuario);
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

        @Override
        public void eliminarTodas(Transaccion tx, int idUsuario) {
            todas.removeIf(o -> o.getIdUsuario() == idUsuario);
        }
    }

    static final class Categorias implements CategoriaGastoDAO {
        final List<CategoriaGasto> todas = new ArrayList<>();

        Categorias con(CategoriaGasto c) {
            todas.add(c);
            return this;
        }

        @Override
        public List<CategoriaGasto> listarTodas() {
            return new ArrayList<>(todas);
        }
    }

    static final class Gastos implements GastoDAO {
        final List<Gasto> todos = new ArrayList<>();
        private int siguienteId = 1;

        @Override
        public int insertar(Gasto gasto) {
            gasto.setIdGasto(siguienteId++);
            todos.add(gasto);
            return gasto.getIdGasto();
        }

        @Override
        public List<Gasto> listarDelMes(int idUsuario, int mes, int anio) {
            return todos.stream()
                    .filter(g -> g.getIdUsuario() == idUsuario && g.getFecha().getMonthValue() == mes
                            && g.getFecha().getYear() == anio)
                    .toList();
        }

        @Override
        public boolean eliminar(int idGasto, int idUsuario) {
            return todos.removeIf(g -> g.getIdGasto() == idGasto && g.getIdUsuario() == idUsuario);
        }

        @Override
        public void convertirImportes(Transaccion tx, int idUsuario, BigDecimal factor) {
            todos.stream().filter(g -> g.getIdUsuario() == idUsuario)
                    .forEach(g -> g.setMonto(convertido(g.getMonto(), factor)));
        }
    }

    /** Calcula el resumen del mes a partir de los gastos en memoria, como hace la consulta SQL. */
    static final class Presupuestos implements PresupuestoDAO {
        final List<Presupuesto> todos = new ArrayList<>();
        private final Gastos gastos;
        private final Categorias categorias;

        Presupuestos(Gastos gastos, Categorias categorias) {
            this.gastos = gastos;
            this.categorias = categorias;
        }

        @Override
        public List<ResumenCategoria> listarResumenMes(int idUsuario, int mes, int anio) {
            return todos.stream()
                    .filter(p -> p.getIdUsuario() == idUsuario && p.getMes() == mes && p.getAnio() == anio)
                    .map(p -> new ResumenCategoria(p.getIdCategoria(), nombre(p.getIdCategoria()), p.getMontoLimite(),
                            gastos.listarDelMes(idUsuario, mes, anio).stream()
                                    .filter(g -> g.getIdCategoria() == p.getIdCategoria())
                                    .map(Gasto::getMonto)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)))
                    .toList();
        }

        @Override
        public void guardar(Transaccion tx, Presupuesto presupuesto) {
            eliminar(tx, presupuesto.getIdUsuario(), presupuesto.getIdCategoria(), presupuesto.getMes(),
                    presupuesto.getAnio());
            todos.add(presupuesto);
        }

        @Override
        public boolean eliminar(Transaccion tx, int idUsuario, int idCategoria, int mes, int anio) {
            return todos.removeIf(p -> p.getIdUsuario() == idUsuario && p.getIdCategoria() == idCategoria
                    && p.getMes() == mes && p.getAnio() == anio);
        }

        @Override
        public void convertirImportes(Transaccion tx, int idUsuario, BigDecimal factor) {
            todos.stream().filter(p -> p.getIdUsuario() == idUsuario)
                    .forEach(p -> p.setMontoLimite(convertido(p.getMontoLimite(), factor)));
        }

        private String nombre(int idCategoria) {
            return categorias.todas.stream().filter(c -> c.getIdCategoria() == idCategoria)
                    .map(CategoriaGasto::getNombre).findFirst().orElse("?");
        }
    }

    static final class Lecciones implements LeccionDAO {
        final List<Leccion> todas = new ArrayList<>();

        Lecciones con(Leccion l) {
            todas.add(l);
            return this;
        }

        /** Mismo orden que la consulta real: por nivel y por identificador. */
        @Override
        public List<Leccion> listarTodas() {
            return todas.stream()
                    .sorted(Comparator.comparing(Leccion::getNivel).thenComparing(Leccion::getIdLeccion))
                    .toList();
        }
    }

    static final class Preguntas implements PreguntaDAO {
        final List<Pregunta> todas = new ArrayList<>();

        Preguntas con(Pregunta p) {
            todas.add(p);
            return this;
        }

        @Override
        public List<Pregunta> listarPorLeccion(int idLeccion) {
            return todas.stream().filter(p -> p.getIdLeccion() == idLeccion).toList();
        }

        @Override
        public Map<Integer, Integer> contarPorLeccion() {
            Map<Integer, Integer> totales = new HashMap<>();
            todas.forEach(p -> totales.merge(p.getIdLeccion(), 1, Integer::sum));
            return totales;
        }
    }

    static final class Progresos implements ProgresoLeccionDAO {
        final List<ProgresoLeccion> todos = new ArrayList<>();
        private int siguienteId = 1;

        @Override
        public List<ProgresoLeccion> listarPorUsuario(int idUsuario) {
            return todos.stream().filter(p -> p.getIdUsuario() == idUsuario).toList();
        }

        @Override
        public Optional<ProgresoLeccion> bloquear(Transaccion tx, int idUsuario, int idLeccion) {
            return todos.stream()
                    .filter(p -> p.getIdUsuario() == idUsuario && p.getIdLeccion() == idLeccion)
                    .findFirst();
        }

        @Override
        public void insertar(Transaccion tx, ProgresoLeccion progreso) {
            progreso.setIdProgreso(siguienteId++);
            todos.add(progreso);
        }

        @Override
        public void actualizar(Transaccion tx, ProgresoLeccion progreso) {
            todos.removeIf(p -> p.getIdProgreso() == progreso.getIdProgreso());
            todos.add(progreso);
        }
    }

    /** Igual que la SQL: GREATEST(ROUND(importe × factor, 2), 0.01). */
    static BigDecimal convertido(BigDecimal importe, BigDecimal factor) {
        return importe.multiply(factor).setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
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
