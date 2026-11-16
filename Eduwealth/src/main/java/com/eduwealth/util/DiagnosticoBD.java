package com.eduwealth.util;

import com.eduwealth.dao.ActivoDAO;
import com.eduwealth.dao.BrokerDAO;
import com.eduwealth.dao.DataAccessException;
import com.eduwealth.dao.DivisaDAO;
import com.eduwealth.dao.UsuarioDAO;
import com.eduwealth.dao.jdbc.ActivoDAOJdbc;
import com.eduwealth.dao.jdbc.BrokerDAOJdbc;
import com.eduwealth.dao.jdbc.DivisaDAOJdbc;
import com.eduwealth.dao.jdbc.UsuarioDAOJdbc;
import com.eduwealth.model.Activo;
import com.eduwealth.model.Broker;
import com.eduwealth.model.Divisa;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Programa de consola que comprueba la conexión con MySQL y la capa DAO
 * mostrando los datos iniciales. No forma parte de la interfaz gráfica.
 * <p>
 * Ejecución: {@code ./mvnw -q compile exec:java -Dexec.mainClass=com.eduwealth.util.DiagnosticoBD}
 */
public final class DiagnosticoBD {

    private DiagnosticoBD() {
    }

    public static void main(String[] args) {
        try {
            DivisaDAO divisaDAO = new DivisaDAOJdbc();
            ActivoDAO activoDAO = new ActivoDAOJdbc();
            UsuarioDAO usuarioDAO = new UsuarioDAOJdbc();
            BrokerDAO brokerDAO = new BrokerDAOJdbc();

            System.out.println("=== Divisas (unidades por 1 EUR) ===");
            Map<Integer, Divisa> divisas = divisaDAO.listarTodas().stream()
                    .collect(Collectors.toMap(Divisa::getIdDivisa, Function.identity()));
            divisas.values().forEach(d ->
                    System.out.printf("  %-4s %-25s %s%n", d.getCodigo(), d.getNombre(), d.getTipoCambioBase()));

            System.out.println("\n=== Catálogo de activos ===");
            for (Activo a : activoDAO.listarTodos()) {
                Divisa divisa = divisas.get(a.getIdDivisa());
                System.out.printf("  %-8s %-36s %-7s %12s %s%n", a.getTicker(), a.getNombre(),
                        a.getTipoActivo(), a.getPrecioActual().toPlainString(), divisa.getSimbolo());
            }

            System.out.println("\n=== Directorio de brokers ===");
            for (Broker b : brokerDAO.listarTodos()) {
                System.out.printf("  %-20s nivel %-12s activos %s%n",
                        b.getNombre(), b.getNivelRecomendado(), b.getTiposActivo());
            }

            System.out.println("\n=== Prueba de login (BCrypt) ===");
            usuarioDAO.buscarPorEmail("demo@eduwealth.com").ifPresentOrElse(u -> {
                System.out.println("  Usuario: " + u + " · saldo " + u.getSaldoFicticio());
                System.out.println("  Contraseña 'demo1234' correcta:  " + BCrypt.checkpw("demo1234", u.getPasswordHash()));
                System.out.println("  Contraseña 'otra' correcta:      " + BCrypt.checkpw("otra", u.getPasswordHash()));
            }, () -> System.out.println("  No existe el usuario demo."));

            System.out.println("\nConexión y capa DAO: OK");
        } catch (DataAccessException e) {
            System.err.println("ERROR: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            System.exit(1);
        }
    }
}
