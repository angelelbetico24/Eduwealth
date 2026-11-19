# EduWealth — guía para Claude

Aplicación de escritorio (Java + JavaFX + MySQL) de educación financiera: simulador de inversiones con dinero ficticio, presupuestos, lecciones con test y directorio de brokers. Trabajo académico (DAIA 2026/27, Proyecto Integral II). Autor: Ángel Muñoz López. **Idioma: español** en la interfaz, el código de dominio, los commits y la documentación.

## Estructura del repositorio

```
Eduwealth/                  <- raíz del repo (git)
├── README.md, LICENSE.md
└── Eduwealth/              <- el proyecto Maven (pom.xml, mvnw, src, docs)
    ├── docs/               analisis-diseno.md, requisitos.md, casos-uso.md,
    │                       cambios-v1.1.md (revisión del diseño), modelo-datos/script-base-datos.sql
    └── src/main/java/com/eduwealth/{model,dao,dao/jdbc,service,controller,util}
        src/main/resources/{fxml,css}, src/test/java
```

La **fuente de verdad del diseño** es el PDF «Documento de Análisis y Diseño v1.0» (el usuario lo tiene; no está en el repo) más `docs/cambios-v1.1.md`, que lo corrige. Si hay conflicto, manda `cambios-v1.1.md`.

## Comandos (ejecutar dentro de `Eduwealth/Eduwealth/`)

| Qué | Comando |
|---|---|
| Compilar | `./mvnw -q -B compile` |
| Tests | `./mvnw -q -B test` (informes en `target/surefire-reports/`) |
| Ejecutar la app | `./mvnw javafx:run` |
| Diagnóstico por consola de la BD | `./mvnw -q compile exec:java -Dexec.mainClass=com.eduwealth.util.DiagnosticoBD` |

- Maven no está instalado en el equipo: usar siempre el wrapper `mvnw`.
- Java 21 (`release 21`); el equipo tiene JDK 27. JavaFX 21.0.6, jBCrypt, mysql-connector-j, JUnit 5.
- Se arranca con `Launcher` (no con `MainApp`) para no necesitar module-path.
- La consola de Windows muestra mal los acentos: es solo visual; los datos están bien en UTF-8.
- Para ver la UI: lanzar la app en segundo plano, hacer captura con PowerShell (`CopyFromScreen`) y guardarla en el scratchpad; cerrar el proceso al terminar.

## Base de datos

- MySQL 8.0.46 local, servicio `MySQL80`, base `eduwealth_db`, usuario de la app `eduwealth`.
- Credenciales en `src/main/resources/db.properties` (**ignorado por git**). Plantilla: `db.properties.example`.
- **NO abrir, leer ni mostrar `db.properties`** (el usuario lo pidió expresamente). `ConexionBD` lo lee por código. Si hay que comprobar la BD, usar la app o `DiagnosticoBD`, que no imprimen la contraseña. Nunca poner contraseñas reales en la plantilla, en commits ni en la conversación.
- El script `docs/modelo-datos/script-base-datos.sql` recrea todo (DROP + CREATE + datos de prueba). Lo ejecuta el usuario desde Workbench como `root`.
- Usuarios de prueba: `demo@eduwealth.com` / `demo1234` (USUARIO) y `admin@eduwealth.com` / `admin1234` (ADMIN). Están documentados a propósito (el profesor debe poder probar la app).

## Arquitectura (3 capas, apartado 6.11 del diseño)

`controller` (JavaFX) → `service` (reglas de negocio) → `dao` (interfaz) / `dao.jdbc` (JDBC) → MySQL.

- Los controladores **solo hablan con servicios**, obtenidos de `FabricaServicios` (inyección manual). Nunca crean DAO ni ejecutan SQL.
- Los servicios no dependen de JavaFX. Reciben DAO por constructor, así se prueban con DAO falsos en memoria (ver `AutenticacionServiceTest`).
- Errores: `DataAccessException` envuelve `SQLException`; `ValidacionException(campo, mensaje)` para datos inválidos (el controlador marca el campo con la clase CSS `campo-error`).
- Dinero y cantidades: **siempre `BigDecimal`** (C-11). Formato en pantalla con `util/Formato` (`10.000,00 €`).
- Navegación: `Navegador.ir("vista")` cambia la pantalla completa; `Navegador.cargar("vista")` carga una vista dentro del área de contenido de `MainController`. Las secciones del menú están en el enum `Seccion` (`vista == null` → muestra «Próximamente»). Para activar un módulo: crear su FXML + controlador y asignar su vista en `Seccion`.
- Estilos: una sola hoja `css/eduwealth.css` con las variables de color del prototipo (azul noche `#0f172a`, primario `#2563eb`, verde `#22c55e`).
- `Sesion` guarda el usuario autenticado (estático).

## Reglas de negocio (RN) y decisiones de la revisión v1.1

RN-01 compra ≤ saldo · RN-02 venta ≤ títulos poseídos · RN-03 presupuesto: <80 % dentro del límite, ≥80 % y <100 % «En Alerta», ≥100 % «Presupuesto Excedido» · RN-04 convertir a la divisa base del usuario · RN-05 test aprobado con ≥70 % · RN-06 sin datos bancarios reales, derivar al broker con enlace externo · **RN-07** preparación = 70·(aprobadas/totales) + 30·min(operaciones/10, 1), «Listo» desde 75 % · **RN-08** lecciones de un nivel se desbloquean al aprobar todas las del anterior.

Decisiones clave (detalle en `cambios-v1.1.md`, C-01…C-23): EUR es la divisa pivote y `tipo_cambio_base` = unidades por 1 EUR · el saldo está en la divisa base del usuario · el precio de compra/venta siempre es `activos.precio_actual` (el usuario solo elige activo y cantidad) · al vender todo se borra la posición · `pnl_realizado` solo en ventas, en divisa del activo · el test guarda la **mejor nota** · categorías de gasto globales · RF-16 es solo «Registro de gastos» (sin ingresos) · RF-19/CU-13 reiniciar simulador (borra posiciones y operaciones, no progreso ni presupuestos) · tabla `preguntas` (A–D) · `usuarios.rol` · `UNIQUE(usuario, categoría, mes, año)` en presupuestos · CHECK de integridad.

Las operaciones que tocan varias tablas (compra, venta, reinicio, test) deben ir en **una transacción** con rollback (caso límite 6.9.5).

## Estado actual (actualizar al avanzar)

Hecho: estructura Maven/VS Code · script SQL v1.1 con datos · 12 entidades · registro y login (CU-01, CU-02) · menú lateral + Panel Principal con datos reales · **Simulador completo** (CU-03…CU-07: catálogo con búsqueda/filtros, compra RN-01, venta RN-02 con P&L realizado, cartera, historial, cotización en vivo) · transacciones (`Transaccion`/`GestorTransacciones`, C-25) · `SimuladorService`, `DivisaService` (RN-04), `PresupuestoService` (RN-03), `EducacionService` (RN-07 parcial), `PanelService` · 22 tests unitarios (todos pasan; DAO falsos compartidos en `src/test/.../DaosEnMemoria.java`).

El usuario demo tiene ya operaciones reales hechas en las pruebas (6 AAPL, saldo 8.921,65 €). Reejecutar el script SQL lo deja como al principio.

Pendiente, en este orden: 1) **Presupuestos y gastos** (CU-10, CU-11: definir límite mensual, registrar gasto, estados RN-03, vista con barras por categoría) · 2) Educación (lecciones, test RN-05, RN-08, mejor nota) · 3) Directorio de brokers con filtros y enlace externo con aviso (RN-06, CU-09) · 4) Configuración (cambio de divisa/contraseña RF-03, reiniciar simulador RF-19) · 5) Panel de administración (CU-12) · 6) README (descripción, tecnologías, instrucciones) · 7) refactorizaciones, optimizaciones, registro de IA y documento v2.

Pruebas de UI: script de ayuda en el scratchpad (`ui.ps1`: foco, clic relativo a la ventana, teclas con SendKeys, captura, cerrar). Si no existe, recrearlo con `user32` (`SetForegroundWindow`, `SetCursorPos`, `mouse_event`) y `System.Drawing`.

## Entregables del enunciado (Proyecto Integral II) — lo que se evalúa

- App funcional y ejecutable; README con instrucciones; documento de análisis y diseño **versión 2** (cambios justificados).
- **Registro de uso de IA** (tabla: Nº | Tarea | Herramienta IA | Objetivo | Resultado y decisión) con las intervenciones relevantes.
- **3 refactorizaciones** documentadas (código inicial, problema, propuesta de la IA, cambios aplicados, justificación) y **3 optimizaciones** (problema, propuesta, por qué es mejor). Se pueden descartar propuestas de la IA explicándolo.
- Historial de Git progresivo: commits pequeños y descriptivos, no un único volcado final.

Candidatas ya detectadas (dejar el «antes» en un commit propio para que el historial lo muestre; no corregirlas antes de documentarlas):
- Refactor: código repetido `buscarUno` / listados en todos los `*DAOJdbc` → clase base con mapeador de filas · los controladores/Fábrica instancian DAO concretos.
- Optimización: `ConexionBD` abre una conexión nueva por consulta (sin pool) · cargas repetidas de divisas/activos (ya se cargan una vez en `PanelService`; hay que medir y documentar) · resumen de presupuestos con una sola consulta (`LEFT JOIN` + `SUM`) frente a una por categoría.

## Forma de trabajar

- Cambios pequeños y verificables: compilar, ejecutar los tests y, si toca la UI, abrir la app y comprobarla antes de dar algo por hecho.
- Un commit por funcionalidad con mensaje descriptivo (`feat(…)`, `docs: …`, `db: …`, `build: …`), en español. Terminar el mensaje con la línea `Co-Authored-By` que indique el entorno. Hacer `git push origin main` tras cada commit que el usuario pida subir.
- Cualquier cambio de diseño, requisitos, modelo de datos o tecnología se **documenta en `docs/cambios-v1.1.md`** (o en el apartado de la v2) con problema, solución y secciones afectadas. El usuario copiará ese contenido a su documento.
- Validaciones en la UI (campo en rojo) **y** en el servicio **y** como CHECK en la BD.
- No añadir frameworks pesados (Spring, Hibernate) ni tablas extra: el usuario rechazó expresamente la complejidad innecesaria.
- Al terminar un bloque, informar del porcentaje aproximado de avance del proyecto (el usuario lo pregunta con frecuencia).
- Notas de refactorización/optimización/IA: apuntar en el momento lo relevante (qué propuso la IA, qué se aceptó o descartó) para alimentar el registro final.
