# 6.17. Revisión del diseño — Versión 1.1

**Proyecto:** EduWealth · **Autor:** Ángel Muñoz López · **Base:** Documento de Análisis y Diseño v1.0 (22/10/2026)

Antes de empezar la implementación (Proyecto Integral II) se ha revisado el documento v1.0 contrastándolo con los prototipos, el script SQL y los requisitos de la segunda entrega. Se han detectado huecos, contradicciones y errores que impedirían implementar algunas funcionalidades o que harían ambigua la lógica de negocio.

Este apartado recoge **qué se cambia, por qué y cuál es la solución adoptada**. El resto del documento v1.0 sigue vigente salvo en los puntos aquí indicados. Estos cambios se integrarán en el documento de análisis y diseño **versión 2**, junto con los que surjan durante la implementación.

---

## Resumen de cambios

| ID | Tipo | Cambio | Secciones afectadas |
|---|---|---|---|
| C-01 | Modelo de datos | Nueva tabla `preguntas` | 6.4 (RF-12), 6.8 (CU-08), 6.10 |
| C-02 | Modelo de datos | Campo `rol` en `usuarios` | 6.7 (CU-02, CU-12), 6.10 |
| C-03 | Modelo de datos | Restricción única en `presupuestos` | 6.10.6 |
| C-04 | Modelo de datos | Campo `pnl_realizado` en `operaciones_simuladas` | 6.4 (RF-06), 6.10 |
| C-05 | Modelo de datos | Eliminación de posiciones a cero | CU-05, 6.12 |
| C-06 | Modelo de datos | Definición de `tipo_cambio_base` (EUR como pivote) | 6.10.2, RN-04 |
| C-07 | Modelo de datos | Moneda del saldo ficticio | RF-01, RF-03, RF-04 |
| C-08 | Modelo de datos | Campo `fecha_actualizacion` en `divisas` | 6.9 (caso 4) |
| C-09 | Modelo de datos | Categorías de gasto globales | CU-11 |
| C-10 | Modelo de datos | Reintentos de test: se guarda la mejor nota | CU-08, RN-05 |
| C-11 | Diseño de clases | `BigDecimal` para importes y cantidades | 6.12 |
| C-22 | Modelo de datos | Campos de filtrado en `brokers` | 6.4 (RF-14), 6.10 |
| C-23 | Modelo de datos | Restricciones `CHECK` de integridad | 6.9, 6.10.6 |
| C-24 | Regla de negocio | Cantidades fraccionarias, redondeo a céntimos e importe mínimo | RF-05, RF-06, CU-04, CU-05 |
| C-25 | Arquitectura | Transacciones en la capa DAO sin acoplar los servicios a JDBC | 6.11, CU-04 (E-02), 6.9.5 |
| C-26 | Requisito / Modelo de datos | Movimiento simulado del mercado por días transcurridos | 6.3, RF-07, CU-06, CU-12, 6.10 |
| C-27 | Caso de uso | Gestión de los límites mensuales de presupuesto | RF-17, CU-10 |
| C-12 | Regla de negocio | Nueva RN-07: fórmula del medidor de preparación | RF-13 |
| C-13 | Regla de negocio | Nueva RN-08: desbloqueo de lecciones por nivel | 6.6, 6.13 |
| C-14 | Requisito | Nuevo RF-19 / CU-13: reiniciar simulador | 6.4, 6.7, 6.13 |
| C-15 | Requisito | Precio de compra tomado del catálogo | RF-05, CU-04 |
| C-16 | Caso de uso | Umbrales sin huecos y pasos numerados | CU-11 |
| C-17 | Requisito | RF-16 pasa a «Registro de gastos», sin «cuentas» | RF-16 |
| C-18 | Diagrama UML | Corrección del diagrama de clases | 6.12 |
| C-19 | Formato | Erratas en la tabla de casos de uso y en RF-05 | 6.4, 6.7 |
| C-20 | Seguridad | Solo BCrypt para contraseñas | RNF-01 |
| C-21 | Tecnologías | Entorno VS Code, Maven y Java 21 | 6.14 |

---

## 1. Cambios en el modelo de datos

### C-01 · Nueva tabla `preguntas`
- **Problema:** RF-12, CU-08 y RN-05 exigen un test con nota final (aprobado ≥ 70 %), pero ninguna tabla almacena las preguntas ni las respuestas correctas. Sin ellas no se puede corregir el test.
- **Solución:** se añade una única tabla con las preguntas tipo test de cada lección (4 opciones, una correcta). Se descarta una tabla separada de respuestas para no añadir complejidad innecesaria.

```sql
CREATE TABLE preguntas (
    id_pregunta INT AUTO_INCREMENT PRIMARY KEY,
    id_leccion INT NOT NULL,
    enunciado VARCHAR(255) NOT NULL,
    opcion_a VARCHAR(150) NOT NULL,
    opcion_b VARCHAR(150) NOT NULL,
    opcion_c VARCHAR(150) NOT NULL,
    opcion_d VARCHAR(150) NOT NULL,
    respuesta_correcta CHAR(1) NOT NULL CHECK (respuesta_correcta IN ('A','B','C','D')),
    FOREIGN KEY (id_leccion) REFERENCES lecciones(id_leccion) ON DELETE CASCADE
);
```
- **Relación nueva:** Leccion 1 : N Pregunta.

### C-02 · Campo `rol` en `usuarios`
- **Problema:** CU-02 da acceso a paneles distintos a usuarios y administradores y CU-12 está reservado al administrador, pero `usuarios` no tiene forma de distinguirlos.
- **Solución:** `rol ENUM('USUARIO','ADMIN') NOT NULL DEFAULT 'USUARIO'`. El script inicial crea un usuario administrador de prueba.

### C-03 · Restricción única en `presupuestos`
- **Problema:** se podían crear dos presupuestos para la misma categoría y mes, con lo que RN-03 no sabría con qué límite comparar.
- **Solución:** `UNIQUE (id_usuario, id_categoria, mes, anio)` y `CHECK (mes BETWEEN 1 AND 12)`.

### C-04 · Campo `pnl_realizado` en `operaciones_simuladas`
- **Problema:** RF-06 pide registrar la plusvalía o minusvalía realizada al vender, pero no hay dónde guardarla. Tampoco puede recalcularse después, porque el precio medio de compra cambia con cada compra posterior.
- **Solución:** `pnl_realizado DECIMAL(15,4) NULL`, calculado en la venta como `(precio_venta − precio_medio_compra) × cantidad` y expresado en la divisa del activo (para mostrarlo en la divisa base se aplica RN-04). Queda a `NULL` en las compras.

### C-05 · Eliminación de posiciones a cero
- **Problema:** no se definía qué ocurre al vender todos los títulos de un activo.
- **Solución:** si `cantidad_titulos` llega a 0, la fila se elimina de `posiciones_cartera` dentro de la misma transacción de la venta.

### C-06 · Definición de `tipo_cambio_base`
- **Problema:** el documento dice «tipo de cambio respecto a la moneda base», pero cada usuario puede tener una moneda base distinta, así que no queda claro respecto a cuál.
- **Solución:** el **EUR es la moneda pivote** del sistema. `tipo_cambio_base` indica cuántas unidades de la divisa equivalen a 1 € (EUR = 1.0000, USD = 1.0850…). La conversión de un importe en la divisa X a la divisa base del usuario es:

  `importe_base = importe_X ÷ tc_X × tc_base`

  RN-04 se aplica siempre con esta fórmula.

### C-07 · Moneda del saldo ficticio
- **Problema:** `saldo_ficticio` vale 10.000 por defecto, pero no se indica en qué moneda, y el usuario puede elegir su divisa base (RF-01) y cambiarla después (RF-03).
- **Solución:** el saldo está siempre expresado en la divisa base del usuario. Al registrarse se asignan 10.000 unidades de esa divisa. Si el usuario cambia de divisa base, el saldo se convierte con la fórmula de C-06.

### C-08 · Campo `fecha_actualizacion` en `divisas`
- **Problema:** el caso límite 4 de 6.9 («no existe tipo de cambio para una divisa») no puede ocurrir: `tipo_cambio_base` es `NOT NULL` y todo activo tiene una divisa obligatoria. El riesgo real es otro: un tipo de cambio **desactualizado**.
- **Solución:** se añade `fecha_actualizacion DATETIME`. Si un tipo de cambio tiene más de 7 días, la interfaz muestra un aviso visual al consolidar el patrimonio. El caso límite 4 se reformula en este sentido.

### C-09 · Categorías de gasto globales
- **Problema:** la precondición de CU-11 exige «una categoría de presupuesto creada por el usuario», pero `categorias_gastos` no tiene `id_usuario`, así que las categorías no pertenecen a nadie.
- **Solución:** las categorías son **globales y predefinidas** (las carga el script inicial). La precondición de CU-11 pasa a ser: *«debe existir un presupuesto definido para esa categoría en el mes actual»*.

### C-10 · Reintentos del test
- **Problema:** `progresos_lecciones` tiene una clave única (usuario, lección), así que un reintento sobrescribiría la nota anterior, incluso con una nota peor.
- **Solución:** se guarda la **mejor nota obtenida**. La nota solo se actualiza si la nueva es mayor, y `completada` pasa a `TRUE` cuando la nota es ≥ 70 (RN-05). Un suspenso nunca desmarca una lección ya aprobada.

### C-11 · `BigDecimal` en el diseño de clases
- **Problema:** el diagrama de clases usa `int` y `double` para títulos y precios, mientras que la base de datos usa `DECIMAL`. `double` produce errores de redondeo con dinero, y `int` no admite fracciones (necesarias en criptomonedas).
- **Solución:** todos los importes, precios, cantidades y tipos de cambio se representan en Java con `BigDecimal`.

### C-22 · Campos de filtrado en `brokers`
- **Problema:** RF-14 pide filtrar los brokers «por comisiones, tipos de activo y nivel recomendado», y el prototipo muestra filtros como «Acciones y ETFs», «Bajas comisiones» o «Apto para principiantes». Sin embargo, la tabla `brokers` solo guarda la información de comisiones como texto libre (`comisiones_info`) y no tiene ningún dato sobre tipos de activo ni nivel, así que no se puede filtrar.
- **Solución:** se añaden tres campos. `comisiones_info` se mantiene como texto descriptivo para la ficha.
  - `comision_minima DECIMAL(6,2)`: comisión mínima por operación en €, para el filtro de bajas comisiones.
  - `tipos_activo SET('ACCION','ETF','FONDO','CRIPTO')`: activos que ofrece el broker. Reutiliza los mismos valores que `activos.tipo_activo`.
  - `nivel_recomendado ENUM('PRINCIPIANTE','INTERMEDIO','AVANZADO')`: reutiliza los niveles de las lecciones, de modo que se puede recomendar un broker acorde al nivel alcanzado por el usuario.

### C-23 · Restricciones `CHECK` de integridad
- **Problema:** las validaciones del apartado 6.9 (cantidades a cero o negativas, importes negativos…) solo se hacían en la interfaz. Un error en el código podría guardar datos imposibles en la base de datos.
- **Solución:** se añaden restricciones `CHECK` como última barrera: cantidades, precios, importes y límites > 0, saldo ≥ 0, mes entre 1 y 12, nota entre 0 y 100 y respuesta correcta entre A y D. Por eso el script requiere MySQL 8.0.16 o superior, que es la versión desde la que se aplican.

### C-24 · Cantidades fraccionarias, redondeo e importe mínimo
- **Problema:** el documento no indica si se pueden comprar fracciones de título, cómo se redondean los importes al convertirlos a la divisa base (el saldo es `DECIMAL(15,2)`) ni qué pasa con órdenes de importe casi nulo, que tras redondear podrían costar 0,00.
- **Solución:**
  - Se admiten **hasta 4 decimales** en la cantidad para cualquier activo, como hacen los brokers que permiten acciones fraccionadas. Es coherente con `DECIMAL(12,4)` y con C-11.
  - El importe de cada orden se calcula en la divisa del activo y, tras convertirlo (RN-04), se **redondea a céntimos** (mitad hacia arriba) antes de restarlo o sumarlo al saldo. El precio medio de compra se guarda con 4 decimales, y el P&L realizado con 4 decimales en la divisa del activo.
  - **Importe mínimo por orden: 0,01** en la divisa base. Una orden más pequeña se rechaza.
  - Los totales de la cartera se muestran redondeados a céntimos. Por el redondeo de cada orden, el patrimonio puede variar en algún céntimo tras comprar y vender al mismo precio, igual que en un broker real.

### C-25 · Transacciones en la capa de acceso a datos
- **Problema:** la compra y la venta modifican tres tablas (`usuarios`, `posiciones_cartera` y `operaciones_simuladas`) y deben ser atómicas (E-02, caso límite 6.9.5). Si los servicios manejaran `Connection` de JDBC directamente, la capa de negocio quedaría acoplada a la tecnología de persistencia, en contra de 6.11.2.
- **Solución:** se añaden las interfaces `Transaccion` y `GestorTransacciones` en el paquete `dao`, con su implementación JDBC en `dao.jdbc`. El servicio abre la transacción con *try-with-resources* y llama a `confirmar()` al final; si se lanza cualquier excepción antes, el cierre hace *rollback* automáticamente. Dentro de la transacción, el saldo y la posición se leen con `SELECT … FOR UPDATE`, de modo que RN-01 y RN-02 se comprueban con datos bloqueados y no con los que había en pantalla.

### C-26 · Movimiento simulado del mercado
- **Problema:** los precios del catálogo eran fijos. Solo cambiaban si el administrador los editaba (CU-12), así que la ganancia o pérdida no realizada (RF-07, CU-06) era siempre 0 y el simulador no enseñaba qué es la volatilidad ni cómo evoluciona una inversión. Conectarse a una API de cotizaciones reales sigue fuera del alcance (apartado 6.3): añadiría dependencia de internet, claves de acceso y límites de uso.
- **Solución:** los precios se mueven de forma **simulada y automática según los días reales transcurridos**.
  - Al iniciar sesión, por cada activo se calculan los días naturales desde su última actualización y se aplica una variación aleatoria por cada día, como máximo 30 días de una vez. Dentro del mismo día no vuelve a cambiar.
  - Cada día el precio se multiplica por `1 + r`, donde `r` sigue una distribución normal con la volatilidad y la tendencia del tipo de activo. La tendencia ligeramente positiva de acciones y fondos (≈ 7 % anual) refleja su crecimiento habitual a largo plazo. Cada variación diaria se limita a ±25 % y el precio nunca baja de 0,0001.

    | Tipo | Volatilidad diaria | Tendencia diaria |
    |---|---|---|
    | Acción | 2 % | +0,03 % |
    | ETF | 1 % | +0,03 % |
    | Fondo | 0,8 % | +0,02 % |
    | Cripto | 5 % | 0 % |

  - **El usuario no puede forzar el mercado.** Un botón para avanzar días permitiría repetir hasta obtener beneficio, y además los precios son comunes a todos los usuarios. Solo el **administrador** tendrá la opción «Simular un día de mercado» (CU-12), pensada para demostraciones.
  - Si dos usuarios inician sesión a la vez, los activos se leen bloqueados (`SELECT … FOR UPDATE`) dentro de una transacción (C-25), y el movimiento se aplica una sola vez.
  - **Modelo de datos:** nueva columna `activos.fecha_actualizacion DATETIME`. Las bases ya creadas se actualizan con `docs/modelo-datos/migraciones/001_C-26_fecha_actualizacion_activos.sql`, sin perder datos.
  - Las reglas RN-01, RN-02 y RN-04 no cambian: las órdenes siguen usando el `precio_actual` del catálogo (C-15).

### C-27 · Gestión de los límites mensuales de presupuesto
- **Problema:** CU-10 solo dice «permite establecer límites máximos de gasto por categoría para un periodo determinado». No explica cómo se modifican ni cómo se quitan, ni qué pasa con los gastos ya anotados si se elimina el límite de su categoría. Esto último choca con C-09, que exige un presupuesto para poder anotar gastos.
- **Solución:**
  - Desde la pantalla de presupuestos, la opción **«Gestionar límites»** muestra todas las categorías del mes seleccionado, cada una con un campo para su límite mensual. **Un campo vacío significa que esa categoría no tiene presupuesto.**
  - **«Copiar mes anterior»** rellena los campos con los límites del mes previo. No se guarda nada hasta pulsar «Guardar límites».
  - Se pueden definir límites del mes actual, de meses pasados (para registrar gastos atrasados) y de meses futuros (para planificar).
  - **Se guarda todo de una vez:** primero se validan todos los campos y después se aplican los cambios en una única transacción (C-25). Si hay un error, se marca el campo de esa categoría y no se guarda nada.
  - **No se puede eliminar el límite de una categoría que ya tiene gastos en ese mes**, porque sus gastos quedarían fuera del control de RN-03. En ese caso se pide ajustar el límite.
  - Crear o ajustar un límite usa `INSERT … ON DUPLICATE KEY UPDATE` sobre la restricción única de C-03, así que nunca se duplican presupuestos del mismo mes.

---

## 2. Cambios en reglas de negocio y requisitos

### C-12 · Nueva RN-07: Medidor de preparación
- **Problema:** RF-13 pide un indicador calculado «en función de los test superados y el rendimiento en el simulador», pero no define la fórmula. El prototipo muestra «75 % Listo».
- **Solución — RN-07:**

  `preparación (%) = 70 × (lecciones aprobadas ÷ lecciones totales) + 30 × min(operaciones realizadas ÷ 10, 1)`

  Si todavía no hay lecciones cargadas, la parte educativa vale 0, para evitar una división por cero. Con un valor ≥ 75 % el usuario se considera **«Listo para la inversión real»** y la aplicación le sugiere el directorio de brokers. Se prima la formación (70 %) sobre la práctica (30 %), porque el objetivo es educativo.

### C-13 · Nueva RN-08: Desbloqueo de lecciones por nivel
- **Problema:** el prototipo de Educación muestra lecciones «Bloqueado (Completa el nivel previo)», pero ninguna regla lo define.
- **Solución — RN-08:** las lecciones de nivel INTERMEDIO se desbloquean al aprobar todas las de nivel PRINCIPIANTE, y las de nivel AVANZADO al aprobar todas las de INTERMEDIO.

### C-14 · Nuevo RF-19 / CU-13: Reiniciar simulador
- **Problema:** el prototipo de Configuración incluye el botón «Reiniciar Saldo Virtual a 10.000,00 €», pero esta función no figuraba entre los requisitos ni los casos de uso.
- **Solución:**
  - **RF-19 – Reinicio del simulador:** el sistema permitirá al usuario restablecer su saldo ficticio a 10.000 unidades de su divisa base.
  - **CU-13 – Reiniciar simulador** (actor: Usuario): tras una confirmación explícita, se eliminan las posiciones y el historial de operaciones del usuario y se restablece el saldo, todo en una única transacción. El progreso educativo y los presupuestos no se ven afectados. Como se borra el historial, la parte del medidor de preparación que depende del simulador (RN-07) también vuelve a cero.

### C-15 · Precio de compra tomado del catálogo
- **Problema:** RF-05 dice que el usuario indica «el precio de adquisición», pero CU-04 usa el precio actual del activo. Si el usuario pudiera elegir el precio, el simulador perdería su sentido.
- **Solución:** el usuario solo indica el **activo y la cantidad**. El precio es siempre el `precio_actual` del catálogo. El término «tiempo real» se entiende como recálculo al abrir o refrescar la vista con los precios que mantiene el administrador. Una API de cotizaciones externas queda fuera del alcance de esta versión.

### C-16 · Umbrales sin huecos en CU-11
- **Problema:** FA-01 indica «entre el 80 % y el 99 %», lo que deja sin cubrir valores como el 99,5 %. Además, los flujos aluden al «paso 6» sin que los pasos estén numerados.
- **Solución:** los rangos pasan a ser **< 80 % → «Dentro del límite»**, **≥ 80 % y < 100 % → «En Alerta»** y **≥ 100 % → «Presupuesto Excedido»**. Los pasos de todos los flujos principales se numeran.

### C-17 · Referencia a «cuentas» en RF-16
- **Problema:** RF-16 dice «asociándolos a cuentas y categorías específicas», pero no existe ninguna entidad «cuenta».
- **Solución:** los gastos se asocian únicamente a una categoría y se expresan en la divisa base del usuario. Además, el modelo de datos, CU-11 y los prototipos solo contemplan gastos, así que RF-16 pasa a llamarse **«Registro de gastos»**. Los ingresos quedan fuera del alcance de esta versión.

---

## 3. Correcciones de diagramas, formato y tecnologías

### C-18 · Diagrama de clases (6.12)
- **Problemas:**
  - `PosicionCartera` aparece dos veces.
  - `Usuario` está dentro de la «Capa de Controlador», y el título tiene una errata («Controlaidor»).
  - Las firmas del diagrama no coinciden con el texto. Por ejemplo, `ejecutarOrdenVenta(usuario, posicion, …)` frente a `(usuario, activo, …)`, y `evaluarUmbral(usuario): boolean` cuando el resultado tiene tres estados posibles.
  - No aparecen la capa DAO ni las entidades `Divisa`, `Leccion`, `Broker` y `Presupuesto`.
- **Solución:** se rehace el diagrama en PlantUML. Se mantiene la arquitectura en 3 capas de 6.11, y el diagrama muestra sus paquetes: modelo (entidades), acceso a datos (DAO), servicios y controladores. Las firmas quedan unificadas, por ejemplo `evaluarUmbral(idUsuario, idCategoria): EstadoPresupuesto`, donde `EstadoPresupuesto` es un enum con los valores `DENTRO_LIMITE`, `EN_ALERTA` y `EXCEDIDO`. Se añaden también la entidad `Pregunta` y un `DivisaService` para RN-04.

### C-19 · Erratas de formato (6.4 y 6.7)
- **Problema:** la fila de CU-02 dice «Autenticar usuarioUsuario», con el actor «Admin». Además, RF-05 dice «indicando el ticket» en lugar de «ticker».
- **Solución:** «Autenticar usuario», con los actores Usuario y Administrador, y «ticker» en RF-05.

### C-20 · Algoritmo de contraseñas (RNF-01)
- **Problema:** la documentación del repositorio mencionaba «BCrypt/SHA-256». SHA-256 sin sal no es adecuado para almacenar contraseñas.
- **Solución:** se utiliza exclusivamente **BCrypt** (librería jBCrypt).

### C-21 · Tecnologías (6.14)
- **Problema:** la tabla de tecnologías indicaba IntelliJ IDEA como entorno, pero la segunda entrega exige Visual Studio Code. La versión de Java tampoco estaba concretada (JDK 17+).
- **Solución:**

| Elemento | v1.0 | v1.1 |
|---|---|---|
| Entorno | IntelliJ IDEA | **Visual Studio Code** + Extension Pack for Java |
| Lenguaje | Java (JDK 17+) | **Java 21 (LTS)**, compatible con la indicación 17+ |
| Gestión de dependencias | Maven | Maven (sin cambios): JavaFX, MySQL Connector/J, jBCrypt |
| Asistente IA | — | Claude Code (integrado en VS Code) |

---

## Nuevas reglas y requisitos tras la revisión

- **RN-07** – Medidor de preparación (C-12)
- **RN-08** – Desbloqueo de lecciones por nivel (C-13)
- **RF-19 / CU-13** – Reiniciar simulador (C-14)
- **Tabla nueva:** `preguntas` (C-01)
- **Campos nuevos:** `usuarios.rol`, `operaciones_simuladas.pnl_realizado`, `divisas.fecha_actualizacion`, `brokers.comision_minima`, `brokers.tipos_activo`, `brokers.nivel_recomendado`
