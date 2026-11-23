package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PosicionCarteraDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.OperacionSimulada;
import com.eduwealth.model.PosicionCartera;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.TipoOperacion;
import com.eduwealth.model.Usuario;
import com.eduwealth.util.Formato;
import com.eduwealth.util.Numeros;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simulador de inversiones (Paper Trading): catálogo (CU-03), compra (CU-04, RN-01),
 * venta (CU-05, RN-02), cartera y P&amp;L (CU-06) e historial (CU-07).
 * <p>
 * El precio de cada orden es siempre el precio actual del catálogo (C-15). Los importes se
 * convierten a la divisa base del usuario antes de tocar su saldo (RN-04).
 */
public class SimuladorService {

    /** Máximo de títulos por orden: cabe en DECIMAL(12,4). */
    static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("99999999");
    static final int DECIMALES_CANTIDAD = 4;
    static final BigDecimal IMPORTE_MINIMO = new BigDecimal("0.01");

    private final UsuarioDAO usuarioDAO;
    private final ActivoDAO activoDAO;
    private final PosicionCarteraDAO posicionDAO;
    private final OperacionSimuladaDAO operacionDAO;
    private final GestorTransacciones transacciones;
    private final DivisaService divisaService;

    public SimuladorService(UsuarioDAO usuarioDAO, ActivoDAO activoDAO, PosicionCarteraDAO posicionDAO,
                            OperacionSimuladaDAO operacionDAO, GestorTransacciones transacciones,
                            DivisaService divisaService) {
        this.usuarioDAO = usuarioDAO;
        this.activoDAO = activoDAO;
        this.posicionDAO = posicionDAO;
        this.operacionDAO = operacionDAO;
        this.transacciones = transacciones;
        this.divisaService = divisaService;
    }

    // ------------------------------------------------------------------ consultas

    public List<Activo> catalogo() {
        return activoDAO.listarTodos();
    }

    public Map<Integer, Divisa> divisasPorId() {
        return divisaService.listarDivisas().stream()
                .collect(Collectors.toMap(Divisa::getIdDivisa, Function.identity()));
    }

    /** CU-03: búsqueda por ticker o nombre y filtros opcionales por tipo y divisa (null = todos). */
    public List<Activo> filtrar(List<Activo> activos, String texto, TipoActivo tipo, Divisa divisa) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return activos.stream()
                .filter(a -> buscado.isEmpty()
                        || a.getTicker().toLowerCase(Locale.ROOT).contains(buscado)
                        || a.getNombre().toLowerCase(Locale.ROOT).contains(buscado))
                .filter(a -> tipo == null || a.getTipoActivo() == tipo)
                .filter(a -> divisa == null || a.getIdDivisa() == divisa.getIdDivisa())
                .toList();
    }

    /** CU-06: cartera con valor de mercado y P&amp;L, y totales consolidados en la divisa base. */
    public ResumenCartera resumenCartera(Usuario usuario, LocalDateTime ahora) {
        // Catálogos pequeños: se cargan una vez en lugar de consultar por cada posición
        Map<Integer, Divisa> divisas = divisasPorId();
        Map<Integer, Activo> activos = activoDAO.listarTodos().stream()
                .collect(Collectors.toMap(Activo::getIdActivo, Function.identity()));
        Divisa base = divisas.get(usuario.getIdDivisaBase());

        List<PosicionDetalle> posiciones = new ArrayList<>();
        BigDecimal valorCartera = BigDecimal.ZERO;
        BigDecimal pnlTotal = BigDecimal.ZERO;
        boolean desactualizadas = false;
        for (PosicionCartera p : posicionDAO.listarPorUsuario(usuario.getIdUsuario())) {
            Activo activo = activos.get(p.getIdActivo());
            Divisa divisa = divisas.get(activo.getIdDivisa());
            BigDecimal precio = activo.getPrecioActual();
            BigDecimal valor = p.calcularValorActual(precio);
            BigDecimal pnl = p.calcularPnLNoRealizado(precio);
            posiciones.add(new PosicionDetalle(activo, divisa, p.getCantidadTitulos(), p.getPrecioMedioCompra(),
                    valor, pnl, p.calcularPorcentajePnL(precio)));
            // RN-04: se convierte a la divisa base antes de sumar al patrimonio
            valorCartera = valorCartera.add(divisaService.convertir(valor, divisa, base));
            pnlTotal = pnlTotal.add(divisaService.convertir(pnl, divisa, base));
            desactualizadas |= divisa.getIdDivisa() != base.getIdDivisa()
                    && divisaService.estaDesactualizada(divisa, ahora);
        }

        // Totales en céntimos, igual que el saldo: evita restos de medio céntimo al comparar con el capital inicial
        valorCartera = aCentimos(valorCartera);
        pnlTotal = aCentimos(pnlTotal);
        BigDecimal saldo = usuario.getSaldoFicticio();
        BigDecimal patrimonio = saldo.add(valorCartera);
        // Capital inicial guardado en la divisa base: la rentabilidad es correcta aunque se cambie de divisa (C-28)
        BigDecimal capitalInicial = usuario.getCapitalInicial();
        BigDecimal rentabilidad = patrimonio.subtract(capitalInicial);
        BigDecimal rentabilidadPct = rentabilidad.multiply(BigDecimal.valueOf(100))
                .divide(capitalInicial, 2, RoundingMode.HALF_UP);
        return new ResumenCartera(base, saldo, valorCartera, pnlTotal, patrimonio, capitalInicial, rentabilidad,
                rentabilidadPct, posiciones, desactualizadas);
    }

    /** CU-07: historial del usuario, de la operación más reciente a la más antigua. */
    public List<OperacionDetalle> historial(int idUsuario) {
        Map<Integer, Divisa> divisas = divisasPorId();
        Map<Integer, Activo> activos = activoDAO.listarTodos().stream()
                .collect(Collectors.toMap(Activo::getIdActivo, Function.identity()));
        return operacionDAO.listarPorUsuario(idUsuario).stream()
                .map(op -> {
                    Activo activo = activos.get(op.getIdActivo());
                    return new OperacionDetalle(op.getFechaHora(), op.getTipoOperacion(), activo.getTicker(),
                            activo.getNombre(), op.getCantidad(), op.getPrecioUnitario(), op.getImporteTotal(),
                            op.getPnlRealizado(), divisas.get(activo.getIdDivisa()).getSimbolo());
                })
                .toList();
    }

    // ------------------------------------------------------------------ validación y vista previa

    /**
     * Convierte el texto introducido en una cantidad válida (caso límite 6.9.1):
     * número positivo, máximo 4 decimales y dentro del rango admitido. Acepta coma o punto decimal.
     */
    public BigDecimal validarCantidad(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("cantidad", "Introduce el número de títulos.");
        }
        BigDecimal cantidad;
        try {
            cantidad = Numeros.leer(texto); // 1.000 son mil títulos, no uno
        } catch (NumberFormatException e) {
            throw new ValidacionException("cantidad", "La cantidad debe ser un número (por ejemplo 10 o 0,5).");
        }
        if (cantidad.signum() <= 0) {
            throw new ValidacionException("cantidad", "La cantidad debe ser mayor que cero.");
        }
        if (cantidad.stripTrailingZeros().scale() > DECIMALES_CANTIDAD) {
            throw new ValidacionException("cantidad", "La cantidad admite como máximo 4 decimales.");
        }
        if (cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
            throw new ValidacionException("cantidad", "La cantidad es demasiado grande.");
        }
        return cantidad;
    }

    /**
     * Vista previa de una orden: importe en ambas divisas y si cumple RN-01 (compra) o RN-02 (venta).
     * No accede a la base de datos, por lo que puede recalcularse en cada pulsación (FA-01).
     */
    public Cotizacion cotizar(TipoOperacion tipo, Activo activo, Divisa divisaActivo, Divisa base,
                              BigDecimal cantidad, BigDecimal saldo, BigDecimal titulosPoseidos) {
        BigDecimal importeActivo = cantidad.multiply(activo.getPrecioActual());
        BigDecimal importeBase = aCentimos(divisaService.convertir(importeActivo, divisaActivo, base));
        boolean ventaTotal = tipo == TipoOperacion.VENTA && cantidad.compareTo(titulosPoseidos) == 0;
        if (importeBase.compareTo(IMPORTE_MINIMO) < 0 && !ventaTotal) {
            return new Cotizacion(importeActivo, importeBase, false, "El importe mínimo de una operación es 0,01.");
        }
        if (tipo == TipoOperacion.COMPRA && importeBase.compareTo(saldo) > 0) {
            return new Cotizacion(importeActivo, importeBase, false, mensajeSaldoInsuficiente(importeBase, saldo, base));
        }
        if (tipo == TipoOperacion.VENTA && cantidad.compareTo(titulosPoseidos) > 0) {
            return new Cotizacion(importeActivo, importeBase, false,
                    mensajeTitulosInsuficientes(activo, titulosPoseidos));
        }
        return new Cotizacion(importeActivo, importeBase, true, null);
    }

    // ------------------------------------------------------------------ operaciones

    /**
     * CU-04: compra simulada. Todo ocurre en una transacción: si algo falla no cambia
     * ni el saldo, ni la cartera, ni el historial (E-02).
     */
    public ResultadoOperacion comprar(int idUsuario, int idActivo, BigDecimal cantidad) {
        try (Transaccion tx = transacciones.iniciar()) {
            Usuario usuario = usuarioDAO.bloquearPorId(tx, idUsuario)
                    .orElseThrow(() -> new ValidacionException("El usuario no existe."));
            Activo activo = buscarActivo(idActivo);
            Divisa divisaActivo = buscarDivisa(activo.getIdDivisa());
            Divisa base = buscarDivisa(usuario.getIdDivisaBase());

            BigDecimal precio = activo.getPrecioActual();
            BigDecimal coste = aCentimos(divisaService.convertir(cantidad.multiply(precio), divisaActivo, base));
            validarImporteMinimo(coste);
            // RN-01: el coste no puede superar el saldo disponible (leído con la fila bloqueada)
            if (coste.compareTo(usuario.getSaldoFicticio()) > 0) {
                throw new ValidacionException("cantidad",
                        mensajeSaldoInsuficiente(coste, usuario.getSaldoFicticio(), base));
            }

            Optional<PosicionCartera> existente = posicionDAO.bloquear(tx, idUsuario, idActivo);
            if (existente.isPresent()) {
                PosicionCartera posicion = existente.get();
                posicion.setPrecioMedioCompra(calcularPrecioMedio(
                        posicion.getCantidadTitulos(), posicion.getPrecioMedioCompra(), cantidad, precio));
                posicion.setCantidadTitulos(posicion.getCantidadTitulos().add(cantidad));
                posicionDAO.actualizar(tx, posicion);
            } else {
                posicionDAO.insertar(tx, new PosicionCartera(0, idUsuario, idActivo, cantidad, precio));
            }

            OperacionSimulada operacion = new OperacionSimulada(0, idUsuario, idActivo, TipoOperacion.COMPRA,
                    cantidad, precio, null, null);
            operacionDAO.insertar(tx, operacion);

            BigDecimal saldoRestante = usuario.getSaldoFicticio().subtract(coste);
            usuarioDAO.actualizarSaldo(tx, idUsuario, saldoRestante);
            tx.confirmar();
            return new ResultadoOperacion(operacion, coste, saldoRestante);
        }
    }

    /**
     * CU-05: venta simulada, parcial o total. Registra la plusvalía o minusvalía realizada (C-04)
     * y elimina la posición si se venden todos los títulos (C-05).
     */
    public ResultadoOperacion vender(int idUsuario, int idActivo, BigDecimal cantidad) {
        try (Transaccion tx = transacciones.iniciar()) {
            Usuario usuario = usuarioDAO.bloquearPorId(tx, idUsuario)
                    .orElseThrow(() -> new ValidacionException("El usuario no existe."));
            Activo activo = buscarActivo(idActivo);
            Divisa divisaActivo = buscarDivisa(activo.getIdDivisa());
            Divisa base = buscarDivisa(usuario.getIdDivisaBase());

            PosicionCartera posicion = posicionDAO.bloquear(tx, idUsuario, idActivo).orElse(null);
            BigDecimal poseidos = posicion == null ? BigDecimal.ZERO : posicion.getCantidadTitulos();
            // RN-02: no se pueden vender más títulos de los que se poseen
            if (posicion == null || cantidad.compareTo(poseidos) > 0) {
                throw new ValidacionException("cantidad", mensajeTitulosInsuficientes(activo, poseidos));
            }

            BigDecimal precio = activo.getPrecioActual();
            BigDecimal ingreso = aCentimos(divisaService.convertir(cantidad.multiply(precio), divisaActivo, base));
            BigDecimal restantes = poseidos.subtract(cantidad);
            // Vender todo siempre está permitido: un resto de valor inferior a 0,01 no debe quedar atrapado (C-24)
            if (restantes.signum() != 0) {
                validarImporteMinimo(ingreso);
            }
            BigDecimal pnlRealizado = precio.subtract(posicion.getPrecioMedioCompra())
                    .multiply(cantidad).setScale(4, RoundingMode.HALF_UP);

            if (restantes.signum() == 0) {
                posicionDAO.eliminar(tx, posicion.getIdPosicion());
            } else {
                posicion.setCantidadTitulos(restantes); // el PMC no cambia al vender
                posicionDAO.actualizar(tx, posicion);
            }

            OperacionSimulada operacion = new OperacionSimulada(0, idUsuario, idActivo, TipoOperacion.VENTA,
                    cantidad, precio, pnlRealizado, null);
            operacionDAO.insertar(tx, operacion);

            BigDecimal saldoRestante = usuario.getSaldoFicticio().add(ingreso);
            usuarioDAO.actualizarSaldo(tx, idUsuario, saldoRestante);
            tx.confirmar();
            return new ResultadoOperacion(operacion, ingreso, saldoRestante);
        }
    }

    /**
     * Precio medio de compra ponderado tras una nueva compra:
     * (títulos × PMC + nuevos × precio) ÷ (títulos + nuevos), con 4 decimales.
     */
    static BigDecimal calcularPrecioMedio(BigDecimal titulos, BigDecimal pmc, BigDecimal nuevos, BigDecimal precio) {
        BigDecimal costeTotal = titulos.multiply(pmc).add(nuevos.multiply(precio));
        return costeTotal.divide(titulos.add(nuevos), 4, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ auxiliares

    private Activo buscarActivo(int idActivo) {
        return activoDAO.buscarPorId(idActivo)
                .orElseThrow(() -> new ValidacionException("El activo seleccionado ya no existe en el catálogo."));
    }

    private Divisa buscarDivisa(int idDivisa) {
        return divisaService.buscarPorId(idDivisa)
                .orElseThrow(() -> new ValidacionException("La divisa del activo no está configurada."));
    }

    private static void validarImporteMinimo(BigDecimal importe) {
        if (importe.compareTo(IMPORTE_MINIMO) < 0) {
            throw new ValidacionException("cantidad", "El importe mínimo de una operación es 0,01.");
        }
    }

    private static BigDecimal aCentimos(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP);
    }

    private static String mensajeSaldoInsuficiente(BigDecimal coste, BigDecimal saldo, Divisa base) {
        return "Saldo virtual insuficiente: la orden cuesta " + Formato.moneda(coste, base.getSimbolo())
                + " y dispones de " + Formato.moneda(saldo, base.getSimbolo()) + ".";
    }

    private static String mensajeTitulosInsuficientes(Activo activo, BigDecimal poseidos) {
        return poseidos.signum() == 0
                ? "No tienes títulos de " + activo.getTicker() + " en tu cartera."
                : "Solo tienes " + poseidos.stripTrailingZeros().toPlainString() + " títulos de "
                + activo.getTicker() + ".";
    }
}
