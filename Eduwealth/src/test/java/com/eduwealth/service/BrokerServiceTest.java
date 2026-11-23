package com.eduwealth.service;

import com.eduwealth.model.Broker;
import com.eduwealth.model.Nivel;
import com.eduwealth.model.TipoActivo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-09: filtros del directorio y validación del enlace oficial (RF-15, RN-06). */
class BrokerServiceTest {

    private final BrokerService servicio = new BrokerService(null);

    private static final Broker IB = broker(1, "Interactive Brokers", "1.25", Nivel.AVANZADO,
            "Central Bank of Ireland · registrado en CNMV", "https://www.interactivebrokers.ie",
            TipoActivo.ACCION, TipoActivo.ETF, TipoActivo.FONDO);
    private static final Broker TR = broker(2, "Trade Republic", "1.00", Nivel.PRINCIPIANTE, "BaFin, Bundesbank",
            "https://traderepublic.com", TipoActivo.ACCION, TipoActivo.ETF, TipoActivo.CRIPTO);
    private static final Broker ETORO = broker(3, "eToro", "0.00", Nivel.PRINCIPIANTE, "CySEC, FCA",
            "https://www.etoro.com", TipoActivo.ACCION, TipoActivo.CRIPTO);
    private static final List<Broker> TODOS = List.of(IB, TR, ETORO);

    @Test
    void filtrosDelDirectorio() {
        assertEquals(3, servicio.filtrar(TODOS, "", BrokerService.Filtro.TODOS).size());
        assertEquals(List.of(IB, TR), servicio.filtrar(TODOS, null, BrokerService.Filtro.ACCIONES_Y_ETF));
        assertEquals(List.of(TR, ETORO), servicio.filtrar(TODOS, null, BrokerService.Filtro.BAJAS_COMISIONES));
        assertEquals(List.of(IB, ETORO), servicio.filtrar(TODOS, null, BrokerService.Filtro.REGULACION_CNMV_FCA));
        assertEquals(List.of(TR, ETORO), servicio.filtrar(TODOS, null, BrokerService.Filtro.PRINCIPIANTES));
        assertEquals(List.of(TR, ETORO), servicio.filtrar(TODOS, null, BrokerService.Filtro.CRIPTO));
    }

    @Test
    void busquedaPorNombreCombinadaConFiltro() {
        assertEquals(List.of(TR), servicio.filtrar(TODOS, "  trade ", null));
        assertTrue(servicio.filtrar(TODOS, "trade", BrokerService.Filtro.REGULACION_CNMV_FCA).isEmpty());
    }

    @Test
    void recomendacionSegunElNivel() {
        assertTrue(servicio.recomendadoPara(TR, Nivel.PRINCIPIANTE));
        assertFalse(servicio.recomendadoPara(IB, Nivel.PRINCIPIANTE));
    }

    @Test
    void soloSeDevuelvenEnlacesOficialesValidos() {
        assertEquals("traderepublic.com", servicio.enlaceOficial(TR).getHost());
        // E-01 de CU-09: enlace ausente o erróneo → servicio no disponible
        for (String malo : new String[]{null, " ", "javascript:alert(1)", "ftp://broker.com", "https://", "no es url"}) {
            Broker b = broker(9, "Roto", "1", Nivel.PRINCIPIANTE, "", malo, TipoActivo.ACCION);
            ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.enlaceOficial(b));
            assertTrue(e.getMessage().contains("no está disponible"), String.valueOf(malo));
        }
    }

    private static Broker broker(int id, String nombre, String comision, Nivel nivel, String regulacion, String url,
                                 TipoActivo... tipos) {
        return new Broker(id, nombre, "descripción de " + nombre, "info", new BigDecimal(comision),
                EnumSet.of(tipos[0], tipos), nivel, regulacion, url);
    }
}
