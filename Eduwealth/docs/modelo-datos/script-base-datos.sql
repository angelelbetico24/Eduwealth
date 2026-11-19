-- ============================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS Y TABLAS - EDUWEALTH
-- Motor: MySQL 8.0.16+ (necesario para que se apliquen los CHECK)
-- Proyecto: EduWealth (Análisis y Diseño v1.1)
--
-- Cambios respecto a v1.0 (ver docs/cambios/cambios-v1.1.md):
--   C-01 tabla preguntas            C-02 usuarios.rol
--   C-03 UNIQUE en presupuestos      C-04 operaciones_simuladas.pnl_realizado
--   C-08 divisas.fecha_actualizacion C-22 campos de filtrado en brokers
--   C-26 activos.fecha_actualizacion (movimiento simulado del mercado)
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
DROP TABLE IF EXISTS preguntas;
DROP TABLE IF EXISTS lecciones;
DROP TABLE IF EXISTS posiciones_cartera;
DROP TABLE IF EXISTS operaciones_simuladas;
DROP TABLE IF EXISTS activos;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS divisas;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. TABLA: divisas
-- tipo_cambio_base = unidades de la divisa por 1 EUR (EUR es la moneda pivote, C-06)
-- ------------------------------------------------------------
CREATE TABLE divisas (
    id_divisa INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(5) NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL,
    simbolo VARCHAR(5) NOT NULL,
    tipo_cambio_base DECIMAL(12,4) NOT NULL,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_divisa_tc CHECK (tipo_cambio_base > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. TABLA: usuarios
-- saldo_ficticio se expresa en la divisa base del usuario (C-07)
-- ------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM('USUARIO', 'ADMIN') NOT NULL DEFAULT 'USUARIO',
    saldo_ficticio DECIMAL(15,2) NOT NULL DEFAULT 10000.00,
    id_divisa_base INT NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_saldo CHECK (saldo_ficticio >= 0),
    FOREIGN KEY (id_divisa_base) REFERENCES divisas(id_divisa) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. TABLA: activos
-- fecha_actualizacion: último cambio de precio; el mercado simulado avanza por días (C-26)
-- ------------------------------------------------------------
CREATE TABLE activos (
    id_activo INT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    tipo_activo ENUM('ACCION', 'ETF', 'FONDO', 'CRIPTO') NOT NULL,
    precio_actual DECIMAL(15,4) NOT NULL,
    id_divisa INT NOT NULL,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_activo_precio CHECK (precio_actual > 0),
    FOREIGN KEY (id_divisa) REFERENCES divisas(id_divisa) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. TABLA: operaciones_simuladas
-- pnl_realizado solo se rellena en las VENTAS, en la divisa del activo (C-04)
-- ------------------------------------------------------------
CREATE TABLE operaciones_simuladas (
    id_operacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_activo INT NOT NULL,
    tipo_operacion ENUM('COMPRA', 'VENTA') NOT NULL,
    cantidad DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(15,4) NOT NULL,
    pnl_realizado DECIMAL(15,4) NULL,
    fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_operacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_operacion_precio CHECK (precio_unitario > 0),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_activo) REFERENCES activos(id_activo) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 5. TABLA: posiciones_cartera
-- Una posición con 0 títulos se elimina (C-05)
-- ------------------------------------------------------------
CREATE TABLE posiciones_cartera (
    id_posicion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_activo INT NOT NULL,
    cantidad_titulos DECIMAL(12,4) NOT NULL,
    precio_medio_compra DECIMAL(15,4) NOT NULL,
    CONSTRAINT uk_usuario_activo UNIQUE (id_usuario, id_activo),
    CONSTRAINT chk_posicion_cantidad CHECK (cantidad_titulos > 0),
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
-- 7. TABLA: preguntas (NUEVA, C-01)
-- ------------------------------------------------------------
CREATE TABLE preguntas (
    id_pregunta INT AUTO_INCREMENT PRIMARY KEY,
    id_leccion INT NOT NULL,
    enunciado VARCHAR(255) NOT NULL,
    opcion_a VARCHAR(150) NOT NULL,
    opcion_b VARCHAR(150) NOT NULL,
    opcion_c VARCHAR(150) NOT NULL,
    opcion_d VARCHAR(150) NOT NULL,
    respuesta_correcta CHAR(1) NOT NULL,
    CONSTRAINT chk_pregunta_respuesta CHECK (respuesta_correcta IN ('A', 'B', 'C', 'D')),
    FOREIGN KEY (id_leccion) REFERENCES lecciones(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 8. TABLA: progresos_lecciones
-- Se guarda la mejor nota obtenida (C-10)
-- ------------------------------------------------------------
CREATE TABLE progresos_lecciones (
    id_progreso INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_leccion INT NOT NULL,
    completada BOOLEAN DEFAULT FALSE,
    nota_test DECIMAL(5,2),
    fecha_completado DATETIME,
    CONSTRAINT uk_usuario_leccion UNIQUE (id_usuario, id_leccion),
    CONSTRAINT chk_progreso_nota CHECK (nota_test BETWEEN 0 AND 100),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_leccion) REFERENCES lecciones(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 9. TABLA: categorias_gastos (globales y predefinidas, C-09)
-- ------------------------------------------------------------
CREATE TABLE categorias_gastos (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 10. TABLA: presupuestos
-- Un único límite por usuario, categoría y mes (C-03)
-- ------------------------------------------------------------
CREATE TABLE presupuestos (
    id_presupuesto INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_categoria INT NOT NULL,
    monto_limite DECIMAL(12,2) NOT NULL,
    mes INT NOT NULL,
    anio INT NOT NULL,
    CONSTRAINT uk_presupuesto_periodo UNIQUE (id_usuario, id_categoria, mes, anio),
    CONSTRAINT chk_presupuesto_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT chk_presupuesto_limite CHECK (monto_limite > 0),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias_gastos(id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 11. TABLA: gastos (en la divisa base del usuario, C-17)
-- ------------------------------------------------------------
CREATE TABLE gastos (
    id_gasto INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_categoria INT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    fecha DATE NOT NULL,
    concepto VARCHAR(150),
    CONSTRAINT chk_gasto_monto CHECK (monto > 0),
    INDEX idx_gastos_usuario_fecha (id_usuario, fecha),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias_gastos(id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 12. TABLA: brokers
-- tipos_activo, comision_minima y nivel_recomendado permiten los filtros de RF-14 (C-22)
-- ------------------------------------------------------------
CREATE TABLE brokers (
    id_broker INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    comisiones_info VARCHAR(255),
    comision_minima DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    tipos_activo SET('ACCION', 'ETF', 'FONDO', 'CRIPTO') NOT NULL,
    nivel_recomendado ENUM('PRINCIPIANTE', 'INTERMEDIO', 'AVANZADO') NOT NULL,
    regulacion VARCHAR(100),
    url_referido VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- ============================================================
-- DATOS INICIALES
-- Precios y tipos de cambio ilustrativos: entorno de simulación.
-- ============================================================

-- Divisas (unidades por 1 EUR)
INSERT INTO divisas (codigo, nombre, simbolo, tipo_cambio_base) VALUES
('EUR', 'Euro', '€', 1.0000),
('USD', 'Dólar estadounidense', '$', 1.0850),
('GBP', 'Libra esterlina', '£', 0.8550);

-- Usuarios de prueba (contraseñas cifradas con BCrypt)
--   admin@eduwealth.com / admin1234  -> rol ADMIN
--   demo@eduwealth.com  / demo1234   -> rol USUARIO
INSERT INTO usuarios (nombre, email, password_hash, rol, id_divisa_base) VALUES
('Administrador', 'admin@eduwealth.com', '$2a$10$OxbvtK.qCdxmuEon.G.ZiuJuy0F6rNH9nGqfAncCGS.3paoriD5IO', 'ADMIN', 1),
('Usuario Demo', 'demo@eduwealth.com', '$2a$10$aGhODfIsoTBOeKC9Sd397upkgBq9D3oJ2dVwWNEt7idAuIpLsvNVK', 'USUARIO', 1);

-- Categorías de gasto
INSERT INTO categorias_gastos (nombre, descripcion) VALUES
('Alimentación', 'Supermercado y compra diaria'),
('Vivienda y Suministros', 'Alquiler, hipoteca, luz, agua e internet'),
('Transporte', 'Combustible, transporte público y mantenimiento'),
('Ocio y Restauración', 'Restaurantes, cine, viajes y suscripciones'),
('Educación', 'Cursos, libros y material formativo'),
('Salud', 'Farmacia, consultas y seguros médicos'),
('Otros', 'Gastos que no encajan en otra categoría');

-- Activos del simulador
INSERT INTO activos (ticker, nombre, tipo_activo, precio_actual, id_divisa) VALUES
('AAPL', 'Apple Inc.', 'ACCION', 195.0000, 2),
('MSFT', 'Microsoft Corp.', 'ACCION', 420.0000, 2),
('NVDA', 'NVIDIA Corp.', 'ACCION', 135.0000, 2),
('SAN', 'Banco Santander', 'ACCION', 3.9800, 1),
('ITX', 'Inditex', 'ACCION', 48.5000, 1),
('VOD', 'Vodafone Group', 'ACCION', 0.7000, 3),
('VWCE', 'Vanguard FTSE All-World UCITS ETF', 'ETF', 115.0000, 1),
('IWDA', 'iShares Core MSCI World UCITS ETF', 'ETF', 88.4000, 1),
('VGLOBAL', 'Vanguard Global Stock Index Fund', 'FONDO', 52.3000, 1),
('BTC', 'Bitcoin', 'CRIPTO', 62500.0000, 2),
('ETH', 'Ethereum', 'CRIPTO', 3200.0000, 2);

-- Brokers del directorio
INSERT INTO brokers (nombre, descripcion, comisiones_info, comision_minima, tipos_activo, nivel_recomendado, regulacion, url_referido) VALUES
('Interactive Brokers', 'Plataforma profesional con acceso a más de 150 mercados internacionales.', 'Desde 1,25 € por operación en acciones europeas', 1.25, 'ACCION,ETF,FONDO', 'AVANZADO', 'Central Bank of Ireland · registrado en CNMV', 'https://www.interactivebrokers.ie'),
('DEGIRO', 'Broker europeo con comisiones reducidas y amplia selección de ETFs.', '1 € por operación + gastos de conectividad', 1.00, 'ACCION,ETF,FONDO', 'INTERMEDIO', 'BaFin, AFM · registrado en CNMV', 'https://www.degiro.es'),
('Trade Republic', 'Aplicación sencilla con planes de inversión periódicos sin comisión.', '1 € por operación; planes de inversión gratuitos', 1.00, 'ACCION,ETF,CRIPTO', 'PRINCIPIANTE', 'BaFin, Bundesbank', 'https://traderepublic.com'),
('eToro', 'Plataforma social con interfaz intuitiva y función de CopyTrading.', 'Sin comisión en acciones; cobra diferencial y cambio de divisa', 0.00, 'ACCION,ETF,CRIPTO', 'PRINCIPIANTE', 'CySEC, FCA', 'https://www.etoro.com');

-- ------------------------------------------------------------
-- Lecciones (2 por nivel) y sus preguntas
-- ------------------------------------------------------------
INSERT INTO lecciones (id_leccion, titulo, contenido, nivel) VALUES
(1, 'Introducción a los Mercados Financieros',
'Los mercados financieros son los lugares (hoy casi siempre electrónicos) donde se compran y venden activos como acciones, bonos, fondos o divisas.\n\nUna ACCIÓN es una pequeña parte de la propiedad de una empresa: si la empresa crece y gana dinero, la acción suele revalorizarse y puede repartir dividendos. Un BONO es un préstamo que haces a una empresa o a un Estado a cambio de un interés.\n\nLas acciones cotizan en bolsas como la Bolsa de Madrid o el NYSE de Nueva York. El precio cambia continuamente según la oferta y la demanda.\n\nPara invertir necesitas un BROKER: un intermediario autorizado que ejecuta tus órdenes de compra y venta. En España, los brokers deben estar supervisados por la CNMV o por el regulador de otro país de la Unión Europea.\n\nRecuerda: toda inversión conlleva riesgo. Rentabilidades pasadas no garantizan rentabilidades futuras.',
'PRINCIPIANTE'),
(2, 'El Interés Compuesto y el Hábito del Ahorro',
'El interés compuesto consiste en reinvertir los beneficios para que, a su vez, generen nuevos beneficios. Con el tiempo, el crecimiento se acelera.\n\nEjemplo: 1.000 € al 7 % anual se convierten en 1.070 € el primer año. El segundo año el 7 % se aplica sobre 1.070 €, no sobre 1.000 €. En 10 años tendrías unos 1.967 €; en 30 años, unos 7.612 €.\n\nLa variable más importante es el TIEMPO: empezar pronto, aunque sea con poco dinero, tiene más efecto que aportar mucho más tarde.\n\nAntes de invertir conviene tener un FONDO DE EMERGENCIA (entre 3 y 6 meses de gastos) y controlar el presupuesto mensual. Invertir dinero que puedes necesitar a corto plazo te obliga a vender en el peor momento.\n\nLa INFLACIÓN reduce el poder adquisitivo del dinero parado: con una inflación del 3 %, 1.000 € en la cuenta compran menos cada año.',
'PRINCIPIANTE'),
(3, 'Gestión del Riesgo y Diversificación',
'El riesgo es la posibilidad de que el resultado de una inversión sea distinto del esperado, incluida la pérdida de parte del capital.\n\nLa DIVERSIFICACIÓN consiste en repartir el dinero entre distintos activos, sectores y países para que un mal resultado no arruine toda la cartera. "No poner todos los huevos en la misma cesta".\n\nA mayor rentabilidad esperada, mayor riesgo: las acciones suelen ser más volátiles que los bonos, y las criptomonedas mucho más que las acciones.\n\nEl PERFIL DE RIESGO depende de tu horizonte temporal, tus objetivos y tu tolerancia a ver caídas. Un horizonte largo permite asumir más volatilidad.\n\nHerramientas básicas: no invertir más de lo que puedes permitirte perder, limitar el peso de cada posición en la cartera y revisar periódicamente la distribución (rebalanceo).',
'INTERMEDIO'),
(4, 'Fondos Indexados y ETFs',
'Un FONDO DE INVERSIÓN reúne el dinero de muchos inversores y lo invierte según una política definida. Un FONDO INDEXADO replica un índice (por ejemplo, el MSCI World) en lugar de intentar batirlo.\n\nUn ETF (Exchange Traded Fund) es un fondo que cotiza en bolsa como una acción: puede comprarse y venderse durante toda la sesión.\n\nVentajas de la gestión indexada: diversificación inmediata con una sola compra, costes muy bajos (TER habitualmente inferior al 0,30 %) y resultados que, a largo plazo, superan a la mayoría de fondos de gestión activa.\n\nEl TER (Total Expense Ratio) es el coste anual del fondo expresado como porcentaje del capital. Un 1 % anual puede parecer poco, pero en 30 años reduce de forma significativa el resultado final por efecto del interés compuesto.\n\nLos ETF de acumulación reinvierten los dividendos automáticamente; los de distribución los reparten al inversor.',
'INTERMEDIO'),
(5, 'Análisis Técnico y Velas Japonesas',
'El análisis técnico estudia los gráficos de precios y volúmenes para identificar tendencias y posibles puntos de entrada o salida.\n\nUna VELA JAPONESA resume la evolución del precio en un periodo: precio de apertura, cierre, máximo y mínimo. El cuerpo va de la apertura al cierre; las mechas marcan el máximo y el mínimo. Una vela alcista (cierre mayor que apertura) suele dibujarse en verde y una bajista en rojo.\n\nUn SOPORTE es un nivel de precio donde la demanda tiende a frenar las caídas; una RESISTENCIA es un nivel donde la oferta tiende a frenar las subidas.\n\nLa MEDIA MÓVIL suaviza el precio calculando la media de los últimos N periodos. Cuando el precio está por encima de su media de 200 sesiones se suele considerar que la tendencia de fondo es alcista.\n\nEl análisis técnico no predice el futuro: trabaja con probabilidades y debe combinarse con una buena gestión del riesgo.',
'AVANZADO'),
(6, 'Análisis Fundamental de Empresas',
'El análisis fundamental busca estimar el valor real de una empresa estudiando sus cuentas, su negocio y su sector, para compararlo con su precio en bolsa.\n\nEl PER (Price to Earnings Ratio) divide el precio de la acción entre el beneficio por acción. Un PER de 15 significa que pagas 15 veces el beneficio anual. Solo tiene sentido compararlo con empresas similares.\n\nEl BPA (beneficio por acción) es el beneficio neto dividido entre el número de acciones. Su crecimiento sostenido es una buena señal.\n\nEl ROE (rentabilidad sobre fondos propios) mide cuánto beneficio genera la empresa con el capital de sus accionistas.\n\nLa DEUDA también importa: una empresa muy endeudada es más vulnerable cuando suben los tipos de interés o caen las ventas.\n\nLa RENTABILIDAD POR DIVIDENDO es el dividendo anual dividido entre el precio de la acción.',
'AVANZADO');

INSERT INTO preguntas (id_leccion, enunciado, opcion_a, opcion_b, opcion_c, opcion_d, respuesta_correcta) VALUES
-- Lección 1
(1, '¿Qué representa una acción?', 'Un préstamo a una empresa', 'Una parte de la propiedad de una empresa', 'Un depósito bancario', 'Un seguro de vida', 'B'),
(1, '¿Qué es un bono?', 'Una parte de la propiedad de una empresa', 'Un tipo de criptomoneda', 'Un préstamo a una empresa o Estado a cambio de interés', 'Una comisión del broker', 'C'),
(1, '¿Qué función cumple un broker?', 'Ejecutar tus órdenes de compra y venta', 'Garantizar beneficios', 'Fijar el precio de las acciones', 'Emitir moneda', 'A'),
(1, '¿Qué organismo supervisa los mercados de valores en España?', 'El Banco Mundial', 'La CNMV', 'La OCDE', 'El FMI', 'B'),
(1, '¿Qué determina principalmente el precio de una acción en bolsa?', 'El Gobierno', 'El broker', 'La oferta y la demanda', 'El número de empleados', 'C'),
-- Lección 2
(2, '¿En qué consiste el interés compuesto?', 'En cobrar intereses solo sobre el capital inicial', 'En reinvertir los beneficios para que generen nuevos beneficios', 'En pedir un préstamo para invertir', 'En repartir el dinero entre varios bancos', 'B'),
(2, '¿Cuál es la variable más importante del interés compuesto?', 'El tiempo', 'El nombre del broker', 'La divisa', 'El número de operaciones', 'A'),
(2, '¿Cuánto se recomienda tener en el fondo de emergencia?', 'Nada, todo debe invertirse', 'Una semana de gastos', 'Entre 3 y 6 meses de gastos', 'Diez años de gastos', 'C'),
(2, '¿Qué efecto tiene la inflación sobre el dinero parado?', 'Aumenta su valor', 'No le afecta', 'Reduce su poder adquisitivo', 'Lo convierte en acciones', 'C'),
(2, '1.000 € al 7 % anual, ¿cuánto valen tras el primer año?', '1.007 €', '1.070 €', '1.700 €', '7.000 €', 'B'),
-- Lección 3
(3, '¿Qué es la diversificación?', 'Invertir todo en un único activo', 'Repartir la inversión entre distintos activos, sectores y países', 'Vender cuando el precio baja', 'Invertir solo en criptomonedas', 'B'),
(3, 'En general, a mayor rentabilidad esperada...', 'Menor riesgo', 'Mayor riesgo', 'El riesgo no cambia', 'Riesgo nulo', 'B'),
(3, '¿Cuál de estos activos suele ser más volátil?', 'Bonos del Estado', 'Depósitos bancarios', 'Criptomonedas', 'Letras del Tesoro', 'C'),
(3, '¿Qué es rebalancear una cartera?', 'Venderlo todo', 'Volver a ajustar el peso de cada activo a la distribución objetivo', 'Cambiar de broker', 'Pedir un préstamo', 'B'),
(3, '¿Qué permite un horizonte temporal largo?', 'Asumir más volatilidad', 'Evitar cualquier pérdida', 'No pagar comisiones', 'Garantizar beneficios', 'A'),
-- Lección 4
(4, '¿Qué hace un fondo indexado?', 'Intenta batir al mercado eligiendo acciones', 'Replica un índice', 'Invierte solo en una empresa', 'Garantiza el capital', 'B'),
(4, '¿Qué diferencia principal tiene un ETF frente a un fondo tradicional?', 'Cotiza en bolsa como una acción', 'No tiene costes', 'Solo invierte en bonos', 'No se puede vender', 'A'),
(4, '¿Qué es el TER?', 'El precio de una participación', 'El coste anual del fondo en porcentaje', 'La rentabilidad garantizada', 'El número de partícipes', 'B'),
(4, '¿Qué hace un ETF de acumulación con los dividendos?', 'Los reparte al inversor', 'Los reinvierte automáticamente', 'Los dona', 'Los destina a comisiones', 'B'),
(4, '¿Cuál es una ventaja de la gestión indexada?', 'Costes bajos y diversificación inmediata', 'Rentabilidad garantizada', 'Ausencia total de riesgo', 'Comisiones más altas', 'A'),
-- Lección 5
(5, '¿Qué información resume una vela japonesa?', 'Solo el precio de cierre', 'Apertura, cierre, máximo y mínimo', 'El beneficio de la empresa', 'El número de accionistas', 'B'),
(5, 'Una vela alcista es aquella en la que...', 'El cierre es mayor que la apertura', 'El cierre es menor que la apertura', 'No hay mechas', 'El volumen es cero', 'A'),
(5, '¿Qué es un soporte?', 'Un nivel donde la demanda tiende a frenar las caídas', 'Un nivel donde la oferta frena las subidas', 'Una comisión fija', 'Un tipo de orden', 'A'),
(5, '¿Qué es una resistencia?', 'Un nivel donde la demanda frena las caídas', 'Un nivel donde la oferta tiende a frenar las subidas', 'La media de 200 sesiones', 'Un tipo de ETF', 'B'),
(5, '¿Qué calcula una media móvil?', 'El precio máximo histórico', 'La media del precio de los últimos N periodos', 'El dividendo anual', 'El número de operaciones', 'B'),
-- Lección 6
(6, '¿Qué mide el PER?', 'Precio de la acción entre beneficio por acción', 'Deuda entre activos', 'Dividendo entre precio', 'Ventas entre empleados', 'A'),
(6, '¿Qué es el BPA?', 'Beneficio neto entre número de acciones', 'Precio entre dividendo', 'Deuda total', 'Rentabilidad del índice', 'A'),
(6, '¿Qué mide el ROE?', 'La rentabilidad sobre los fondos propios', 'El precio objetivo', 'La volatilidad', 'El número de accionistas', 'A'),
(6, '¿Por qué es importante el nivel de deuda de una empresa?', 'No es relevante', 'Una empresa muy endeudada es más vulnerable', 'Más deuda siempre es mejor', 'Solo afecta a los bancos', 'B'),
(6, '¿Cómo se calcula la rentabilidad por dividendo?', 'Dividendo anual entre precio de la acción', 'Precio entre beneficio', 'Beneficio entre ventas', 'Dividendo por número de acciones', 'A');

-- ------------------------------------------------------------
-- Datos de ejemplo del usuario demo: presupuestos y gastos del mes actual
-- (permiten ver los tres estados de RN-03)
-- ------------------------------------------------------------
INSERT INTO presupuestos (id_usuario, id_categoria, monto_limite, mes, anio) VALUES
(2, 4, 500.00, MONTH(CURDATE()), YEAR(CURDATE())),
(2, 2, 300.00, MONTH(CURDATE()), YEAR(CURDATE())),
(2, 3, 200.00, MONTH(CURDATE()), YEAR(CURDATE()));

INSERT INTO gastos (id_usuario, id_categoria, monto, fecha, concepto) VALUES
(2, 4, 392.50, CURDATE(), 'Cena y entradas de cine'),
(2, 4, 32.50, CURDATE(), 'Comida con amigos'),
(2, 2, 300.00, CURDATE(), 'Alquiler (parte proporcional)'),
(2, 3, 95.00, CURDATE(), 'Abono transporte y gasolina');

