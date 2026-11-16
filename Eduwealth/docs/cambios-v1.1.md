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
- **Campos nuevos:** `usuarios.rol`, `operaciones_simuladas.pnl_realizado`, `divisas.fecha_actualizacion`
