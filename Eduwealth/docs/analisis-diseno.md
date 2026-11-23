# Análisis y diseño — EduWealth (resumen, versión 1.1)

- **Proyecto:** EduWealth — plataforma de educación financiera con simulador de inversiones, presupuestos y directorio de brokers.
- **Autor:** Ángel Muñoz López.
- **Base:** *Documento de Análisis y Diseño v1.0* (22/10/2026), con la revisión **v1.1** aplicada durante la implementación (Proyecto Integral II).

Este documento resume el diseño vigente. El detalle de cada cambio respecto a la v1.0, con su problema, su solución y las secciones afectadas, está en [`cambios/cambios-v1.1.md`](cambios/cambios-v1.1.md) (C-01 a C-29).

| Documento | Contenido |
|---|---|
| [`requisitos.md`](requisitos.md) | RF-01…RF-19, RNF-01…RNF-06, RN-01…RN-08 y matriz de trazabilidad |
| [`casos-uso.md`](casos-uso.md) | CU-01…CU-13 y especificación de los principales |
| [`modelo-datos/script-base-datos.sql`](modelo-datos/script-base-datos.sql) | Script completo de MySQL con datos de prueba |
| [`modelo-datos/migraciones/`](modelo-datos/migraciones/) | Cambios de esquema para bases ya creadas (C-26, C-28) |
| [`prototipos-figma/enlace-figma.md`](prototipos-figma/enlace-figma.md) | Prototipos de la interfaz |

---

## 1. Módulos

| Módulo | Funcionalidad | Casos de uso |
|---|---|---|
| Usuarios y perfil | Registro, login con BCrypt, nombre, contraseña y divisa base | CU-01, CU-02 |
| Simulador (Paper Trading) | Catálogo, compra y venta con dinero ficticio, cartera con P&L, historial, mercado simulado por días, reinicio | CU-03…CU-07, CU-13 |
| Multidivisa | EUR como pivote, conversión a la divisa base y aviso de tipos de cambio desactualizados | — (RN-04) |
| Educación | Ruta por niveles, lecciones, test con corrección y medidor de preparación | CU-08 |
| Brokers | Directorio con filtros y salida segura a la web oficial | CU-09 |
| Presupuestos | Límites mensuales por categoría, gastos y alertas al 80 % y al 100 % | CU-10, CU-11 |
| Administración | Tipos de cambio, precios, simulación de mercado, brokers, lecciones y preguntas | CU-12 |

## 2. Arquitectura en 3 capas

```text
 Presentación   controller/  + resources/fxml + resources/css   (JavaFX)
      │            solo habla con servicios, obtenidos de FabricaServicios
      ▼
 Negocio        service/      reglas RN-01…RN-08, cálculos y validaciones
      │            no depende de JavaFX ni de JDBC; recibe los DAO por constructor
      ▼
 Datos          dao/ (interfaces) + dao/jdbc/ (implementaciones JDBC)
      │            únicas clases que ejecutan SQL; Transaccion / GestorTransacciones (C-25)
      ▼
 Persistencia   MySQL 8 (claves foráneas, UNIQUE y CHECK)
```

- **Paquetes:** `model` (entidades), `dao`, `dao.jdbc`, `service`, `controller` y `util` (conexión, sesión, navegación y formato).
- **Inyección de dependencias manual:** `FabricaServicios` es el único sitio donde se eligen las implementaciones JDBC. Gracias a ello, las pruebas sustituyen los DAO por versiones en memoria.
- **Transacciones:** las operaciones que modifican varias filas o tablas usan una `Transaccion`, con lectura bloqueada (`FOR UPDATE`) y *rollback* automático si algo falla. Son la compra, la venta, el reinicio del simulador, el cambio de divisa, el guardado de límites, el resultado del test y el movimiento de precios.
- **Importes:** siempre `BigDecimal` (C-11). El saldo se redondea a céntimos y las cantidades admiten hasta 4 decimales (C-24).

## 3. Modelo de datos (12 tablas)

`divisas`, `usuarios`, `activos`, `operaciones_simuladas`, `posiciones_cartera`, `lecciones`, `preguntas`, `progresos_lecciones`, `categorias_gastos`, `presupuestos`, `gastos` y `brokers`.

Cambios respecto a la v1.0:
- **Tabla nueva:** `preguntas` (C-01).
- **Columnas nuevas:**
  - `usuarios.rol` (C-02) y `usuarios.capital_inicial` (C-28).
  - `operaciones_simuladas.pnl_realizado` (C-04).
  - `divisas.fecha_actualizacion` (C-08) y `activos.fecha_actualizacion` (C-26).
  - `brokers.comision_minima`, `tipos_activo` y `nivel_recomendado` (C-22).
- **Restricciones:** `UNIQUE (usuario, categoría, mes, año)` en presupuestos (C-03) y restricciones `CHECK` de integridad (C-23).

## 4. Tecnologías

| Elemento | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Interfaz | JavaFX 21 (FXML + CSS); prototipos en Figma |
| Persistencia | MySQL 8 con JDBC (MySQL Connector/J) |
| Seguridad | BCrypt (jBCrypt) |
| Construcción | Maven con *wrapper* (`mvnw`) |
| Pruebas | JUnit 5 (pruebas unitarias de los servicios con DAO en memoria) |
| Entorno | Visual Studio Code (C-21) |
| Control de versiones | Git y GitHub |
