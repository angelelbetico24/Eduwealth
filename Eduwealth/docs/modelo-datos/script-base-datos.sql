-- ============================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS Y TABLAS - EDUWEALTH
-- Motor: MySQL 8.0 / MariaDB
-- Proyecto: EduWealth (Análisis y Diseño v1.0)
-- ============================================================

CREATE DATABASE IF NOT EXISTS eduwealth_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE eduwealth_db;

-- Deshabilitar la comprobación de claves foráneas para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS brokers;
DROP TABLE IF EXISTS gastos;
DROP TABLE IF EXISTS presupuestos;
DROP TABLE IF EXISTS categorias_gastos;
DROP TABLE IF EXISTS progresos_lecciones;
DROP TABLE IF EXISTS lecciones;
DROP TABLE IF EXISTS posiciones_cartera;
DROP TABLE IF EXISTS operaciones_simuladas;
DROP TABLE IF EXISTS activos;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS divisas;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. TABLA: divisas
-- ------------------------------------------------------------
CREATE TABLE divisas (
    id_divisa INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(5) NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL,
    simbolo VARCHAR(5) NOT NULL,
    tipo_cambio_base DECIMAL(12,4) NOT NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. TABLA: usuarios
-- ------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    saldo_ficticio DECIMAL(15,2) NOT NULL DEFAULT 10000.00,
    id_divisa_base INT NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_divisa_base) REFERENCES divisas(id_divisa) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. TABLA: activos
-- ------------------------------------------------------------
CREATE TABLE activos (
    id_activo INT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    tipo_activo ENUM('ACCION', 'ETF', 'FONDO', 'CRIPTO') NOT NULL,
    precio_actual DECIMAL(15,4) NOT NULL,
    id_divisa INT NOT NULL,
    FOREIGN KEY (id_divisa) REFERENCES divisas(id_divisa) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. TABLA: operaciones_simuladas
-- ------------------------------------------------------------
CREATE TABLE operaciones_simuladas (
    id_operacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_activo INT NOT NULL,
    tipo_operacion ENUM('COMPRA', 'VENTA') NOT NULL,
    cantidad DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(15,4) NOT NULL,
    fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_activo) REFERENCES activos(id_activo) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 5. TABLA: posiciones_cartera
-- ------------------------------------------------------------
CREATE TABLE posiciones_cartera (
    id_posicion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_activo INT NOT NULL,
    cantidad_titulos DECIMAL(12,4) NOT NULL,
    precio_medio_compra DECIMAL(15,4) NOT NULL,
    CONSTRAINT uk_usuario_activo UNIQUE (id_usuario, id_activo),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_activo) REFERENCES activos(id_activo) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 6. TABLA: lecciones
-- ------------------------------------------------------------
CREATE TABLE lecciones (
    id_leccion INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    contenido TEXT NOT NULL,
    nivel ENUM('PRINCIPIANTE', 'INTERMEDIO', 'AVANZADO') NOT NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 7. TABLA: progresos_lecciones
-- ------------------------------------------------------------
CREATE TABLE progresos_lecciones (
    id_progreso INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_leccion INT NOT NULL,
    completada BOOLEAN DEFAULT FALSE,
    nota_test DECIMAL(5,2),
    fecha_completado DATETIME,
    CONSTRAINT uk_usuario_leccion UNIQUE (id_usuario, id_leccion),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_leccion) REFERENCES lecciones(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 8. TABLA: categorias_gastos
-- ------------------------------------------------------------
CREATE TABLE categorias_gastos (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 9. TABLA: presupuestos
-- ------------------------------------------------------------
CREATE TABLE presupuestos (
    id_presupuesto INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_categoria INT NOT NULL,
    monto_limite DECIMAL(12,2) NOT NULL,
    mes INT NOT NULL,
    anio INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias_gastos(id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 10. TABLA: gastos
-- ------------------------------------------------------------
CREATE TABLE gastos (
    id_gasto INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_categoria INT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    fecha DATE NOT NULL,
    concepto VARCHAR(150),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias_gastos(id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 11. TABLA: brokers
-- ------------------------------------------------------------
CREATE TABLE brokers (
    id_broker INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    comisiones_info VARCHAR(255),
    regulacion VARCHAR(100),
    url_referido VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- ============================================================
-- INSERCIÓN DE DATOS INICIALES DE PRUEBA
-- ============================================================

-- Divisas principales
INSERT INTO divisas (codigo, nombre, simbolo, tipo_cambio_base) VALUES
('EUR', 'Euro', '€', 1.0000),
('USD', 'Dólar Estadounidense', '$', 1.0850),
('GBP', 'Libra Esterlina', '£', 0.8550);

-- Categorías predeterminadas de gastos
INSERT INTO categorias_gastos (nombre, descripcion) VALUES
('Alimentación', 'Supermercado, restaurantes y comida'),
('Vivienda', 'Alquiler, hipoteca, luz, agua e internet'),
('Transporte', 'Gasolina, transporte público y mantenimiento'),
('Ocio y Entretenimiento', 'Cine, viajes, suscripciones y hobbies'),
('Educación', 'Cursos, libros y material formativo');

-- Activos financieros para simulación
INSERT INTO activos (ticker, nombre, tipo_activo, precio_actual, id_divisa) VALUES
('AAPL', 'Apple Inc.', 'ACCION', 185.5000, 2),
('MSFT', 'Microsoft Corp.', 'ACCION', 415.2000, 2),
('IWDA', 'iShares Core MSCI World ETF', 'ETF', 88.4000, 1),
('BTC', 'Bitcoin', 'CRIPTO', 62500.0000, 2);

-- Lecciones educativas iniciales
INSERT INTO lecciones (titulo, contenido, nivel) VALUES
('Introducción al Interés Compuesto', 'Aprende cómo hacer crecer tu capital a largo plazo gracias al interés compuesto.', 'PRINCIPIANTE'),
('Gestión del Riesgo y Diversificación', 'Conceptos fundamentales para no arriesgar más capital del necesario en tus inversiones.', 'INTERMEDIO'),
('Análisis Fundamental de Acciones', 'Cómo leer e interpretar el balance de una empresa antes de comprar sus títulos.', 'AVANZADO');

-- Brokers regulados del directorio
INSERT INTO brokers (nombre, descripcion, comisiones_info, regulacion, url_referido) VALUES
('Interactive Brokers', 'Plataforma profesional con acceso global a mercados.', 'Desde 0.005$ por acción', 'SEC, FCA, CySEC', 'https://www.interactivebrokers.com'),
('DEGIRO', 'Broker europeo con comisiones muy reducidas para ETFs.', '1€ por operación en selección global', 'BaFin, AFM', 'https://www.degiro.es'),
('Trade Republic', 'Plataforma móvil con planes de inversión automatizados.', '1€ por ejecución', 'BaFin', 'https://traderepublic.com');