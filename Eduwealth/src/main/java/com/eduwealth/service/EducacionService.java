package com.eduwealth.service;

import com.eduwealth.dao.GestorTransacciones;
import com.eduwealth.dao.LeccionDAO;
import com.eduwealth.dao.OperacionSimuladaDAO;
import com.eduwealth.dao.PreguntaDAO;
import com.eduwealth.dao.ProgresoLeccionDAO;
import com.eduwealth.dao.Transaccion;
import com.eduwealth.model.Leccion;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.Pregunta;
import com.eduwealth.model.ProgresoLeccion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Módulo educativo (CU-08): ruta de aprendizaje con desbloqueo por niveles (RN-08), corrección
 * de los test con aprobado al 70 % (RN-05), mejor nota (C-10) y medidor de preparación (RN-07).
 */
public class EducacionService {

    /** Nota mínima para aprobar un test (RN-05). */
    public static final BigDecimal NOTA_APROBADO = new BigDecimal("70");
    /** Preparación a partir de la cual el usuario está «Listo para la inversión real» (RN-07). */
    public static final int UMBRAL_LISTO = 75;
    private static final int PESO_EDUCACION = 70;
    private static final int PESO_SIMULADOR = 30;
    private static final int OPERACIONES_OBJETIVO = 10;
    private static final int PALABRAS_POR_MINUTO = 150;

    private final LeccionDAO leccionDAO;
    private final ProgresoLeccionDAO progresoDAO;
    private final PreguntaDAO preguntaDAO;
    private final OperacionSimuladaDAO operacionDAO;
    private final GestorTransacciones transacciones;

    public EducacionService(LeccionDAO leccionDAO, ProgresoLeccionDAO progresoDAO, PreguntaDAO preguntaDAO,
                            OperacionSimuladaDAO operacionDAO, GestorTransacciones transacciones) {
        this.leccionDAO = leccionDAO;
        this.progresoDAO = progresoDAO;
        this.preguntaDAO = preguntaDAO;
        this.operacionDAO = operacionDAO;
        this.transacciones = transacciones;
    }

    // ------------------------------------------------------------------ consultas

    public List<Leccion> listarLecciones() {
        return leccionDAO.listarTodas();
    }

    /** Identificadores de las lecciones aprobadas (nota ≥ 70 %, RN-05) por el usuario. */
    public Set<Integer> leccionesAprobadas(int idUsuario) {
        return progresoDAO.listarPorUsuario(idUsuario).stream()
                .filter(ProgresoLeccion::isCompletada)
                .map(ProgresoLeccion::getIdLeccion)
                .collect(Collectors.toSet());
    }

    /** Progreso educativo y medidor de preparación del usuario (RN-07). */
    public ResumenEducacion resumen(int idUsuario) {
        List<Leccion> lecciones = listarLecciones();
        List<ProgresoLeccion> progresos = progresoDAO.listarPorUsuario(idUsuario);
        Set<Integer> aprobadas = progresos.stream().filter(ProgresoLeccion::isCompletada)
                .map(ProgresoLeccion::getIdLeccion).collect(Collectors.toSet());
        int preparacion = calcularPreparacion(aprobadas.size(), lecciones.size(),
                operacionDAO.contarPorUsuario(idUsuario));
        Leccion siguiente = siguienteLeccion(lecciones, aprobadas).orElse(null);
        return new ResumenEducacion(aprobadas.size(), lecciones.size(), preparacion, estaListo(preparacion),
                notaMedia(progresos), siguiente, siguiente != null ? siguiente.getNivel() : Nivel.AVANZADO);
    }

    /** Todas las lecciones ordenadas por nivel con su estado para el usuario (superada, disponible, bloqueada). */
    public List<LeccionRuta> rutaDeAprendizaje(int idUsuario) {
        List<Leccion> lecciones = listarLecciones();
        Map<Integer, ProgresoLeccion> progresos = progresoDAO.listarPorUsuario(idUsuario).stream()
                .collect(Collectors.toMap(ProgresoLeccion::getIdLeccion, Function.identity()));
        Set<Integer> aprobadas = progresos.values().stream().filter(ProgresoLeccion::isCompletada)
                .map(ProgresoLeccion::getIdLeccion).collect(Collectors.toSet());
        Set<Nivel> desbloqueados = nivelesDesbloqueados(lecciones, aprobadas);
        Map<Integer, Integer> preguntas = preguntaDAO.contarPorLeccion();

        List<LeccionRuta> ruta = new ArrayList<>();
        for (Leccion l : lecciones) {
            ProgresoLeccion p = progresos.get(l.getIdLeccion());
            Disponibilidad disponibilidad = aprobadas.contains(l.getIdLeccion()) ? Disponibilidad.SUPERADA
                    : desbloqueados.contains(l.getNivel()) ? Disponibilidad.DISPONIBLE : Disponibilidad.BLOQUEADA;
            int numPreguntas = preguntas.getOrDefault(l.getIdLeccion(), 0);
            ruta.add(new LeccionRuta(l, numPreguntas, minutosEstimados(l, numPreguntas), disponibilidad,
                    p == null ? null : p.getNotaTest()));
        }
        return ruta;
    }

    /** Lección para estudiar (CU-08 paso 2). Falla si no existe o está bloqueada (RN-08). */
    public Leccion abrirLeccion(int idUsuario, int idLeccion) {
        List<Leccion> lecciones = listarLecciones();
        Leccion leccion = lecciones.stream().filter(l -> l.getIdLeccion() == idLeccion).findFirst()
                .orElseThrow(() -> new ValidacionException("La lección ya no está disponible."));
        if (!nivelesDesbloqueados(lecciones, leccionesAprobadas(idUsuario)).contains(leccion.getNivel())) {
            throw new ValidacionException("Esta lección está bloqueada: aprueba antes todas las lecciones del nivel "
                    + nombreNivel(nivelAnterior(leccion.getNivel())) + ".");
        }
        return leccion;
    }

    /** Preguntas del test de una lección accesible (CU-08 paso 4). */
    public List<Pregunta> preguntasDelTest(int idUsuario, int idLeccion) {
        abrirLeccion(idUsuario, idLeccion);
        List<Pregunta> preguntas = preguntaDAO.listarPorLeccion(idLeccion);
        if (preguntas.isEmpty()) {
            throw new ValidacionException("Esta lección todavía no tiene test.");
        }
        return preguntas;
    }

    // ------------------------------------------------------------------ CU-08: corregir test

    /**
     * Corrige el test y guarda el resultado. La nota es el porcentaje de aciertos; se aprueba con
     * un 70 % o más (RN-05). Se conserva siempre la mejor nota y un suspenso nunca quita una
     * lección ya aprobada (C-10). El guardado va en una transacción con la fila bloqueada.
     *
     * @param respuestas id de pregunta → opción elegida (A-D); deben estar todas respondidas
     */
    public ResultadoTest corregirTest(int idUsuario, int idLeccion, Map<Integer, Character> respuestas,
                                     LocalDateTime ahora) {
        Leccion leccion = abrirLeccion(idUsuario, idLeccion);
        List<Pregunta> preguntas = preguntasDelTest(idUsuario, idLeccion);
        long sinResponder = preguntas.stream().filter(p -> !respuestaValida(respuestas.get(p.getIdPregunta()))).count();
        if (sinResponder > 0) {
            throw new ValidacionException("Responde todas las preguntas antes de enviar el test (falta"
                    + (sinResponder == 1 ? " 1)." : "n " + sinResponder + ")."));
        }

        List<ResultadoTest.Correccion> correcciones = new ArrayList<>();
        int aciertos = 0;
        for (Pregunta p : preguntas) {
            char elegida = Character.toUpperCase(respuestas.get(p.getIdPregunta()));
            boolean acierto = p.esCorrecta(elegida);
            aciertos += acierto ? 1 : 0;
            correcciones.add(new ResultadoTest.Correccion(p, elegida, acierto));
        }
        BigDecimal nota = BigDecimal.valueOf(aciertos * 100L)
                .divide(BigDecimal.valueOf(preguntas.size()), 2, RoundingMode.HALF_UP);
        boolean aprobado = nota.compareTo(NOTA_APROBADO) >= 0;

        List<Leccion> lecciones = listarLecciones();
        Set<Integer> aprobadasAntes = leccionesAprobadas(idUsuario);

        BigDecimal mejorNota;
        boolean nuevaMejor;
        try (Transaccion tx = transacciones.iniciar()) {
            Optional<ProgresoLeccion> previo = progresoDAO.bloquear(tx, idUsuario, idLeccion);
            if (previo.isEmpty()) {
                progresoDAO.insertar(tx, new ProgresoLeccion(0, idUsuario, idLeccion, aprobado, nota,
                        aprobado ? ahora : null));
                mejorNota = nota;
                nuevaMejor = true;
            } else {
                ProgresoLeccion p = previo.get();
                BigDecimal anterior = p.getNotaTest();
                nuevaMejor = anterior == null || nota.compareTo(anterior) > 0;
                mejorNota = nuevaMejor ? nota : anterior;
                boolean completada = p.isCompletada() || aprobado;
                // La fecha de completado es la del primer aprobado
                LocalDateTime fecha = p.getFechaCompletado() != null ? p.getFechaCompletado() : (aprobado ? ahora : null);
                if (nuevaMejor || completada != p.isCompletada()) {
                    p.setNotaTest(mejorNota);
                    p.setCompletada(completada);
                    p.setFechaCompletado(fecha);
                    progresoDAO.actualizar(tx, p);
                }
            }
            tx.confirmar();
        }

        Nivel desbloqueado = null;
        if (aprobado && !aprobadasAntes.contains(idLeccion)) {
            Set<Integer> aprobadasDespues = new HashSet<>(aprobadasAntes);
            aprobadasDespues.add(idLeccion);
            Set<Nivel> nuevos = nivelesDesbloqueados(lecciones, aprobadasDespues);
            nuevos.removeAll(nivelesDesbloqueados(lecciones, aprobadasAntes));
            desbloqueado = nuevos.stream().findFirst().orElse(null);
        }
        return new ResultadoTest(leccion, aciertos, preguntas.size(), nota, aprobado, mejorNota, nuevaMejor,
                desbloqueado, correcciones);
    }

    // ------------------------------------------------------------------ reglas

    /**
     * RN-07: {@code 70 × (aprobadas ÷ totales) + 30 × min(operaciones ÷ 10, 1)}, redondeado a entero.
     * Sin lecciones cargadas, la parte educativa vale 0 (C-12).
     */
    public int calcularPreparacion(int aprobadas, int totales, int operaciones) {
        double educacion = totales == 0 ? 0 : (double) aprobadas / totales;
        double simulador = Math.min((double) operaciones / OPERACIONES_OBJETIVO, 1.0);
        return (int) Math.round(PESO_EDUCACION * educacion + PESO_SIMULADOR * simulador);
    }

    public boolean estaListo(int preparacion) {
        return preparacion >= UMBRAL_LISTO;
    }

    /** Primera lección, en orden de nivel, que el usuario aún no ha aprobado. */
    public Optional<Leccion> siguienteLeccion(List<Leccion> lecciones, Set<Integer> aprobadas) {
        return lecciones.stream().filter(l -> !aprobadas.contains(l.getIdLeccion())).findFirst();
    }

    /**
     * RN-08: un nivel está desbloqueado si se han aprobado todas las lecciones de los niveles
     * anteriores. Principiante siempre lo está; un nivel sin lecciones no bloquea a los siguientes.
     */
    public Set<Nivel> nivelesDesbloqueados(List<Leccion> lecciones, Set<Integer> aprobadas) {
        Set<Nivel> desbloqueados = EnumSet.noneOf(Nivel.class);
        for (Nivel nivel : Nivel.values()) {
            desbloqueados.add(nivel);
            boolean completo = lecciones.stream()
                    .filter(l -> l.getNivel() == nivel)
                    .allMatch(l -> aprobadas.contains(l.getIdLeccion()));
            if (!completo) {
                break;
            }
        }
        return desbloqueados;
    }

    // ------------------------------------------------------------------ auxiliares

    private static boolean respuestaValida(Character c) {
        return c != null && "ABCD".indexOf(Character.toUpperCase(c)) >= 0;
    }

    /** Media de las mejores notas de las lecciones intentadas, sobre 10 y con 1 decimal. */
    private static BigDecimal notaMedia(List<ProgresoLeccion> progresos) {
        List<BigDecimal> notas = progresos.stream().map(ProgresoLeccion::getNotaTest)
                .filter(n -> n != null).toList();
        if (notas.isEmpty()) {
            return null;
        }
        BigDecimal suma = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(notas.size() * 10L), 1, RoundingMode.HALF_UP);
    }

    /** Tiempo orientativo: lectura a 150 palabras por minuto más un minuto por pregunta. */
    static int minutosEstimados(Leccion leccion, int preguntas) {
        int palabras = leccion.getContenido() == null ? 0 : leccion.getContenido().trim().split("\\s+").length;
        return (int) Math.ceil((double) palabras / PALABRAS_POR_MINUTO) + preguntas;
    }

    private static Nivel nivelAnterior(Nivel nivel) {
        return nivel.ordinal() == 0 ? nivel : Nivel.values()[nivel.ordinal() - 1];
    }

    public static String nombreNivel(Nivel nivel) {
        return switch (nivel) {
            case PRINCIPIANTE -> "Principiante";
            case INTERMEDIO -> "Intermedio";
            case AVANZADO -> "Avanzado";
        };
    }
}
