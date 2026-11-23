package com.eduwealth.service;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.LeccionDAO;
import com.eduwealth.dao.PreguntaDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.TipoActivo;
import com.eduwealth.model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Administración del sistema (CU-12), reservada al rol ADMIN (C-02): tipos de cambio, precios
 * del catálogo y simulación del mercado (C-26), directorio de brokers y catálogo de lecciones
 * con sus preguntas. Todas las operaciones comprueban primero que el usuario es administrador.
 */
public class AdminService {

    /** Código de la divisa pivote: su tipo de cambio es siempre 1 (C-06). */
    public static final String DIVISA_PIVOTE = "EUR";

    private final DivisaDAO divisaDAO;
    private final ActivoDAO activoDAO;
    private final BrokerDAO brokerDAO;
    private final LeccionDAO leccionDAO;
    private final PreguntaDAO preguntaDAO;
    private final GestorTransacciones transacciones;
    private final MercadoService mercado;
    private final BrokerService brokerService;

    public AdminService(DivisaDAO divisaDAO, ActivoDAO activoDAO, BrokerDAO brokerDAO, LeccionDAO leccionDAO,
                        PreguntaDAO preguntaDAO, GestorTransacciones transacciones, MercadoService mercado,
                        BrokerService brokerService) {
        this.divisaDAO = divisaDAO;
        this.activoDAO = activoDAO;
        this.brokerDAO = brokerDAO;
        this.leccionDAO = leccionDAO;
        this.preguntaDAO = preguntaDAO;
        this.transacciones = transacciones;
        this.mercado = mercado;
        this.brokerService = brokerService;
    }

    /** CU-12 es exclusivo del administrador. */
    public void comprobarAdmin(Usuario usuario) {
        if (usuario == null || !usuario.esAdmin()) {
            throw new ValidacionException("Esta sección está reservada al administrador.");
        }
    }

    // ------------------------------------------------------------------ mercado y divisas

    public List<Divisa> divisas(Usuario admin) {
        comprobarAdmin(admin);
        return divisaDAO.listarTodas();
    }

    /** Actualiza el tipo de cambio (unidades por 1 EUR) y su fecha (C-08). El EUR no se puede cambiar. */
    public BigDecimal actualizarTipoCambio(Usuario admin, Divisa divisa, String texto, LocalDateTime ahora) {
        comprobarAdmin(admin);
        if (DIVISA_PIVOTE.equals(divisa.getCodigo())) {
            throw new ValidacionException("tipo-" + divisa.getIdDivisa(),
                    "El EUR es la divisa pivote: su tipo de cambio es siempre 1.");
        }
        BigDecimal tipo = numero(texto, "tipo-" + divisa.getIdDivisa(), "El tipo de cambio", 4,
                new BigDecimal("99999999.9999"));
        divisaDAO.actualizarTipoCambio(divisa.getIdDivisa(), tipo, ahora);
        return tipo;
    }

    public List<Activo> activos(Usuario admin) {
        comprobarAdmin(admin);
        return activoDAO.listarTodos();
    }

    /** Corrige a mano el precio de un activo (C-15); cuenta como actualización del día (C-26). */
    public BigDecimal actualizarPrecio(Usuario admin, Activo activo, String texto, LocalDateTime ahora) {
        comprobarAdmin(admin);
        BigDecimal precio = numero(texto, "precio-" + activo.getIdActivo(), "El precio", 4,
                new BigDecimal("99999999999.9999"));
        try (Transaccion tx = transacciones.iniciar()) {
            activoDAO.actualizarPrecio(tx, activo.getIdActivo(), precio, ahora);
            tx.confirmar();
        }
        return precio;
    }

    /** Botón «Simular un día de mercado» (C-26): solo el administrador puede forzar el mercado. */
    public void simularDiaDeMercado(Usuario admin, LocalDateTime ahora) {
        comprobarAdmin(admin);
        mercado.simularUnDia(ahora);
    }

    // ------------------------------------------------------------------ brokers

    public List<Broker> brokers(Usuario admin) {
        comprobarAdmin(admin);
        return brokerDAO.listarTodos();
    }

    /** Crea (id 0) o actualiza un broker del directorio tras validar todos sus campos. */
    public Broker guardarBroker(Usuario admin, int idBroker, String nombre, String descripcion, String comisionesInfo,
                                String comisionMinima, Set<TipoActivo> tipos, Nivel nivel, String regulacion,
                                String url) {
        comprobarAdmin(admin);
        Broker b = new Broker(idBroker,
                texto(nombre, "nombre", "El nombre", 100, true),
                texto(descripcion, "descripcion", "La descripción", 2000, false),
                texto(comisionesInfo, "comisiones", "La información de comisiones", 255, false),
                numeroOCero(comisionMinima),
                tipos, nivel,
                texto(regulacion, "regulacion", "La regulación", 100, false),
                texto(url, "url", "El enlace oficial", 255, true));
        if (tipos == null || tipos.isEmpty()) {
            throw new ValidacionException("tipos", "Marca al menos un tipo de activo.");
        }
        if (nivel == null) {
            throw new ValidacionException("nivel", "Selecciona el nivel recomendado.");
        }
        try {
            brokerService.enlaceOficial(b); // misma validación que se usa al redirigir (RF-15)
        } catch (ValidacionException e) {
            throw new ValidacionException("url", "El enlace oficial debe ser una dirección web válida (https://…).");
        }
        if (idBroker == 0) {
            brokerDAO.insertar(b);
        } else {
            brokerDAO.actualizar(b);
        }
        return b;
    }

    public void eliminarBroker(Usuario admin, int idBroker) {
        comprobarAdmin(admin);
        if (!brokerDAO.eliminar(idBroker)) {
            throw new ValidacionException("Ese broker ya no existe.");
        }
    }

    // ------------------------------------------------------------------ lecciones y preguntas

    public List<Leccion> lecciones(Usuario admin) {
        comprobarAdmin(admin);
        return leccionDAO.listarTodas();
    }

    /** Crea (id 0) o actualiza una lección. */
    public Leccion guardarLeccion(Usuario admin, int idLeccion, String titulo, Nivel nivel, String contenido) {
        comprobarAdmin(admin);
        Leccion l = new Leccion(idLeccion, texto(titulo, "titulo", "El título", 150, true),
                texto(contenido, "contenido", "El contenido", 60000, true), nivel);
        if (nivel == null) {
            throw new ValidacionException("nivel", "Selecciona el nivel de la lección.");
        }
        if (idLeccion == 0) {
            leccionDAO.insertar(l);
        } else {
            leccionDAO.actualizar(l);
        }
        return l;
    }

    /** Elimina la lección con sus preguntas y el progreso de los usuarios en ella (FK en cascada). */
    public void eliminarLeccion(Usuario admin, int idLeccion) {
        comprobarAdmin(admin);
        if (!leccionDAO.eliminar(idLeccion)) {
            throw new ValidacionException("Esa lección ya no existe.");
        }
    }

    public List<Pregunta> preguntas(Usuario admin, int idLeccion) {
        comprobarAdmin(admin);
        return preguntaDAO.listarPorLeccion(idLeccion);
    }

    /** Crea (id 0) o actualiza una pregunta tipo test con cuatro opciones y una correcta (C-01). */
    public Pregunta guardarPregunta(Usuario admin, int idPregunta, int idLeccion, String enunciado, String a,
                                    String b, String c, String d, Character correcta) {
        comprobarAdmin(admin);
        if (correcta == null || "ABCD".indexOf(Character.toUpperCase(correcta)) < 0) {
            throw new ValidacionException("correcta", "Indica cuál es la respuesta correcta (A, B, C o D).");
        }
        Pregunta p = new Pregunta(idPregunta, idLeccion,
                texto(enunciado, "enunciado", "El enunciado", 255, true),
                texto(a, "opcionA", "La opción A", 150, true),
                texto(b, "opcionB", "La opción B", 150, true),
                texto(c, "opcionC", "La opción C", 150, true),
                texto(d, "opcionD", "La opción D", 150, true),
                Character.toUpperCase(correcta));
        if (idPregunta == 0) {
            preguntaDAO.insertar(p);
        } else {
            preguntaDAO.actualizar(p);
        }
        return p;
    }

    public void eliminarPregunta(Usuario admin, int idPregunta) {
        comprobarAdmin(admin);
        if (!preguntaDAO.eliminar(idPregunta)) {
            throw new ValidacionException("Esa pregunta ya no existe.");
        }
    }

    // ------------------------------------------------------------------ validación

    private static String texto(String valor, String campo, String nombre, int maximo, boolean obligatorio) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            if (obligatorio) {
                throw new ValidacionException(campo, nombre + " es obligatorio.");
            }
            return null;
        }
        if (limpio.length() > maximo) {
            throw new ValidacionException(campo, nombre + " no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static BigDecimal numero(String texto, String campo, String nombre, int decimales, BigDecimal maximo) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException(campo, nombre + " es obligatorio.");
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacionException(campo, nombre + " debe ser un número.");
        }
        if (valor.signum() <= 0) {
            throw new ValidacionException(campo, nombre + " debe ser mayor que cero.");
        }
        if (valor.stripTrailingZeros().scale() > decimales) {
            throw new ValidacionException(campo, nombre + " admite como máximo " + decimales + " decimales.");
        }
        if (valor.compareTo(maximo) > 0) {
            throw new ValidacionException(campo, nombre + " es demasiado grande.");
        }
        return valor.setScale(decimales, RoundingMode.UNNECESSARY);
    }

    /** Comisión mínima: vacía o 0 significa «sin comisión mínima»; nunca negativa. */
    private static BigDecimal numeroOCero(String texto) {
        if (texto == null || texto.isBlank() || texto.strip().replace(',', '.').matches("0+(\\.0+)?")) {
            return BigDecimal.ZERO.setScale(2);
        }
        return numero(texto, "comision", "La comisión mínima", 2, new BigDecimal("9999.99"));
    }
}
