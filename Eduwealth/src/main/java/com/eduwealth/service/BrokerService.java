package com.eduwealth.service;

import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.TipoActivo;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Directorio de brokers regulados (CU-09, RF-14) y derivación a su web oficial (RF-15, RN-06).
 * EduWealth nunca procesa datos bancarios: solo valida y devuelve el enlace oficial.
 */
public class BrokerService {

    /** Comisión mínima por operación hasta la que un broker se considera «de bajas comisiones». */
    public static final BigDecimal UMBRAL_BAJAS_COMISIONES = new BigDecimal("1.00");

    /** Filtros del directorio (prototipo 6.13). */
    public enum Filtro {
        TODOS("Todos", b -> true),
        ACCIONES_Y_ETF("Acciones y ETFs", b -> b.ofrece(TipoActivo.ACCION) && b.ofrece(TipoActivo.ETF)),
        BAJAS_COMISIONES("Bajas comisiones",
                b -> b.getComisionMinima() != null && b.getComisionMinima().compareTo(UMBRAL_BAJAS_COMISIONES) <= 0),
        REGULACION_CNMV_FCA("Regulación CNMV/FCA", b -> contiene(b.getRegulacion(), "CNMV")
                || contiene(b.getRegulacion(), "FCA")),
        CRIPTO("Criptomonedas", b -> b.ofrece(TipoActivo.CRIPTO)),
        PRINCIPIANTES("Apto para principiantes", b -> b.getNivelRecomendado() == Nivel.PRINCIPIANTE);

        private final String etiqueta;
        private final Predicate<Broker> condicion;

        Filtro(String etiqueta, Predicate<Broker> condicion) {
            this.etiqueta = etiqueta;
            this.condicion = condicion;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    private final BrokerDAO brokerDAO;

    public BrokerService(BrokerDAO brokerDAO) {
        this.brokerDAO = brokerDAO;
    }

    public List<Broker> listar() {
        return brokerDAO.listarTodos();
    }

    /** Búsqueda por nombre o descripción y filtro del directorio, en memoria. */
    public List<Broker> filtrar(List<Broker> brokers, String texto, Filtro filtro) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        Filtro aplicado = filtro == null ? Filtro.TODOS : filtro;
        return brokers.stream()
                .filter(b -> buscado.isEmpty() || contiene(b.getNombre(), buscado) || contiene(b.getDescripcion(), buscado))
                .filter(aplicado.condicion)
                .toList();
    }

    /** Si el broker está recomendado para el nivel en el que se forma el usuario. */
    public boolean recomendadoPara(Broker broker, Nivel nivelUsuario) {
        return broker.getNivelRecomendado() == nivelUsuario;
    }

    /**
     * RF-15 / RN-06: devuelve el enlace oficial validado (solo http/https con dominio).
     * Si falta o no es válido, se informa de que el servicio no está disponible (E-01 de CU-09).
     */
    public URI enlaceOficial(Broker broker) {
        String url = broker == null ? null : broker.getUrlReferido();
        if (url == null || url.isBlank()) {
            throw new ValidacionException(noDisponible(broker));
        }
        try {
            URI uri = new URI(url.trim());
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(esquema.equals("https") || esquema.equals("http")) || uri.getHost() == null
                    || !uri.getHost().contains(".")) {
                throw new ValidacionException(noDisponible(broker));
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new ValidacionException(noDisponible(broker));
        }
    }

    private static String noDisponible(Broker broker) {
        String nombre = broker == null ? "El broker" : broker.getNombre();
        return "El enlace de " + nombre + " no está disponible temporalmente. Inténtalo más tarde.";
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(buscado.toLowerCase(Locale.ROOT));
    }
}
