package com.eduwealth.util;

import com.eduwealth.dao.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Proporciona conexiones JDBC a MySQL a partir de {@code db.properties}.
 * <p>
 * Las credenciales se leen del classpath ({@code src/main/resources/db.properties}),
 * un archivo excluido del repositorio. La plantilla es {@code db.properties.example}.
 * Cada llamada a {@link #obtenerConexion()} abre una conexión nueva que el
 * llamante debe cerrar (try-with-resources).
 */
public final class ConexionBD {

    private static final String ARCHIVO_CONFIG = "/db.properties";

    private static Properties config;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() {
        Properties props = cargarConfig();
        try {
            return DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo conectar con la base de datos. Comprueba que MySQL está en marcha "
                            + "y que los datos de db.properties son correctos.", e);
        }
    }

    private static synchronized Properties cargarConfig() {
        if (config == null) {
            try (InputStream in = ConexionBD.class.getResourceAsStream(ARCHIVO_CONFIG)) {
                if (in == null) {
                    throw new DataAccessException(
                            "Falta src/main/resources/db.properties. Cópialo desde db.properties.example "
                                    + "y rellena tus credenciales.");
                }
                Properties props = new Properties();
                props.load(in);
                config = props;
            } catch (IOException e) {
                throw new DataAccessException("No se pudo leer db.properties.", e);
            }
        }
        return config;
    }
}
