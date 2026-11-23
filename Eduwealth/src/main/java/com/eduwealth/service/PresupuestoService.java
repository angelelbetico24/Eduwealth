package com.eduwealth.service;

import com.eduwealth.dao.CategoriaGastoDAO;
import com.eduwealth.dao.GastoDAO;
import com.eduwealth.dao.PresupuestoDAO;
import com.eduwealth.model.CategoriaGasto;
import com.eduwealth.model.EstadoPresupuesto;
import com.eduwealth.model.Gasto;
import com.eduwealth.model.Presupuesto;
import com.eduwealth.model.ResumenCategoria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Presupuestos y control de gastos: definir límites mensuales (CU-10), registrar gastos
 * y evaluar los umbrales de RN-03 (CU-11, rangos sin huecos según C-16).
 */
public class PresupuestoService {

    public static final BigDecimal UMBRAL_ALERTA = new BigDecimal("80");
    public static final BigDecimal UMBRAL_EXCEDIDO = new BigDecimal("100");
    /** Máximo que cabe en DECIMAL(12,2). */
    static final BigDecimal IMPORTE_MAXIMO = new BigDecimal("9999999999.99");
    static final int LONGITUD_MAXIMA_CONCEPTO = 150;
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private final PresupuestoDAO presupuestoDAO;
    private final GastoDAO gastoDAO;
    private final CategoriaGastoDAO categoriaDAO;

    public PresupuestoService(PresupuestoDAO presupuestoDAO, GastoDAO gastoDAO, CategoriaGastoDAO categoriaDAO) {
        this.presupuestoDAO = presupuestoDAO;
        this.gastoDAO = gastoDAO;
        this.categoriaDAO = categoriaDAO;
    }

    // ------------------------------------------------------------------ consultas

    public List<CategoriaGasto> categorias() {
        return categoriaDAO.listarTodas();
    }

    /** Presupuestos del mes con lo gastado en cada categoría, su estado y los totales. */
    public ResumenPresupuestos resumenDelMes(int idUsuario, YearMonth mes) {
        List<CategoriaPresupuesto> categorias = presupuestoDAO
                .listarResumenMes(idUsuario, mes.getMonthValue(), mes.getYear()).stream()
                .map(this::evaluar)
                .toList();
        BigDecimal limite = categorias.stream().map(CategoriaPresupuesto::limite).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gastado = categorias.stream().map(CategoriaPresupuesto::gastado).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenPresupuestos(mes, limite, gastado, limite.subtract(gastado), porcentaje(gastado, limite),
                categorias.isEmpty() ? null : evaluarUmbral(gastado, limite), categorias);
    }

    public List<Gasto> gastosDelMes(int idUsuario, YearMonth mes) {
        return gastoDAO.listarDelMes(idUsuario, mes.getMonthValue(), mes.getYear());
    }

    // ------------------------------------------------------------------ CU-10: definir presupuesto

    /** Crea o ajusta el límite mensual de una categoría. */
    public void definirPresupuesto(int idUsuario, CategoriaGasto categoria, String importe, YearMonth mes) {
        if (categoria == null) {
            throw new ValidacionException("categoria", "Selecciona una categoría.");
        }
        if (mes == null) {
            throw new ValidacionException("mes", "Selecciona el mes del presupuesto.");
        }
        BigDecimal limite = validarImporte(importe, "limite");
        presupuestoDAO.guardar(new Presupuesto(0, idUsuario, categoria.getIdCategoria(), limite,
                mes.getMonthValue(), mes.getYear()));
    }

    public void eliminarPresupuesto(int idUsuario, int idCategoria, YearMonth mes) {
        if (!presupuestoDAO.eliminar(idUsuario, idCategoria, mes.getMonthValue(), mes.getYear())) {
            throw new ValidacionException("Ese presupuesto ya no existe.");
        }
    }

    // ------------------------------------------------------------------ CU-11: registrar gasto

    /**
     * Registra un gasto y devuelve el nuevo estado de su categoría (RN-03).
     * Precondición (C-09): debe existir un presupuesto para esa categoría en el mes del gasto.
     */
    public ResultadoGasto registrarGasto(int idUsuario, CategoriaGasto categoria, String importe, LocalDate fecha,
                                         String concepto, LocalDate hoy) {
        if (categoria == null) {
            throw new ValidacionException("categoria", "Selecciona una categoría.");
        }
        BigDecimal monto = validarImporte(importe, "importe"); // E-01
        if (fecha == null) {
            throw new ValidacionException("fecha", "Indica la fecha del gasto.");
        }
        if (fecha.isAfter(hoy)) {
            throw new ValidacionException("fecha", "La fecha del gasto no puede ser futura.");
        }
        String conceptoLimpio = validarConcepto(concepto);

        YearMonth mes = YearMonth.from(fecha);
        ResumenCategoria antes = presupuestoDAO.listarResumenMes(idUsuario, mes.getMonthValue(), mes.getYear())
                .stream()
                .filter(c -> c.idCategoria() == categoria.getIdCategoria())
                .findFirst()
                .orElseThrow(() -> new ValidacionException("categoria", "No tienes presupuesto para "
                        + categoria.getNombre() + " en " + nombreMes(mes) + ". Defínelo antes de anotar gastos."));

        Gasto gasto = new Gasto(0, idUsuario, categoria.getIdCategoria(), monto, fecha, conceptoLimpio);
        gastoDAO.insertar(gasto);

        // El estado posterior se calcula en memoria: no hace falta volver a consultar la base de datos
        EstadoPresupuesto estadoAnterior = evaluarUmbral(antes.gastado(), antes.limite());
        CategoriaPresupuesto despues = evaluar(new ResumenCategoria(antes.idCategoria(), antes.categoria(),
                antes.limite(), antes.gastado().add(monto)));
        return new ResultadoGasto(gasto, despues, estadoAnterior);
    }

    public void eliminarGasto(int idUsuario, int idGasto) {
        if (!gastoDAO.eliminar(idGasto, idUsuario)) {
            throw new ValidacionException("Ese gasto ya no existe.");
        }
    }

    // ------------------------------------------------------------------ reglas y validaciones

    /** Porcentaje gastado sobre el límite, con 1 decimal. */
    public BigDecimal porcentaje(BigDecimal gastado, BigDecimal limite) {
        if (limite.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return gastado.multiply(BigDecimal.valueOf(100)).divide(limite, 1, RoundingMode.HALF_UP);
    }

    /**
     * RN-03: &lt; 80 % dentro del límite; ≥ 80 % y &lt; 100 % en alerta; ≥ 100 % excedido.
     * Se compara el porcentaje sin redondear para que, por ejemplo, el 99,96 % siga en alerta.
     */
    public EstadoPresupuesto evaluarUmbral(BigDecimal gastado, BigDecimal limite) {
        BigDecimal cien = BigDecimal.valueOf(100);
        BigDecimal gastadoPor100 = gastado.multiply(cien);
        if (gastadoPor100.compareTo(limite.multiply(UMBRAL_EXCEDIDO)) >= 0) {
            return EstadoPresupuesto.EXCEDIDO;
        }
        if (gastadoPor100.compareTo(limite.multiply(UMBRAL_ALERTA)) >= 0) {
            return EstadoPresupuesto.EN_ALERTA;
        }
        return EstadoPresupuesto.DENTRO_LIMITE;
    }

    /**
     * Importe en euros (o divisa base) introducido por el usuario: positivo, máximo 2 decimales,
     * acepta coma o punto. E-01 de CU-11 y caso límite 6.9.1.
     */
    public BigDecimal validarImporte(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException(campo, "Introduce un importe.");
        }
        BigDecimal importe;
        try {
            importe = new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacionException(campo, "El importe debe ser un número (por ejemplo 25 o 12,50).");
        }
        if (importe.signum() <= 0) {
            throw new ValidacionException(campo, "El importe debe ser mayor que cero.");
        }
        if (importe.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException(campo, "El importe admite como máximo 2 decimales.");
        }
        if (importe.compareTo(IMPORTE_MAXIMO) > 0) {
            throw new ValidacionException(campo, "El importe es demasiado grande.");
        }
        return importe.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String validarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            return null;
        }
        String limpio = concepto.trim();
        if (limpio.length() > LONGITUD_MAXIMA_CONCEPTO) {
            throw new ValidacionException("concepto", "El concepto no puede superar los 150 caracteres.");
        }
        return limpio;
    }

    private CategoriaPresupuesto evaluar(ResumenCategoria c) {
        return new CategoriaPresupuesto(c.idCategoria(), c.categoria(), c.limite(), c.gastado(),
                c.limite().subtract(c.gastado()), porcentaje(c.gastado(), c.limite()),
                evaluarUmbral(c.gastado(), c.limite()));
    }

    /** «noviembre de 2026». */
    public static String nombreMes(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ES) + " de " + mes.getYear();
    }
}
