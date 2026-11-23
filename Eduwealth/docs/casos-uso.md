# Casos de uso — EduWealth

Casos de uso del **Documento de Análisis y Diseño v1.0** (apartados 6.7 y 6.8) con los cambios de la revisión **v1.1** aplicados ([`cambios/cambios-v1.1.md`](cambios/cambios-v1.1.md)).

**Actores:** Usuario (estudiante registrado) y Administrador (rol `ADMIN`, C-02).

---

## 1. Tabla de casos de uso

| ID | Caso de uso | Actor | Descripción | Pantalla |
|---|---|---|---|---|
| **CU-01** | Registrar usuario | Usuario | Crea la cuenta con su divisa base y 10.000 de saldo virtual. | Registro |
| **CU-02** | Autenticar usuario | Usuario, Administrador | Inicio de sesión y gestión básica del perfil. | Login, Configuración |
| **CU-03** | Consultar catálogo de activos | Usuario | Búsqueda por ticker o nombre y filtros por tipo y divisa. | Simulador |
| **CU-04** | Ejecutar orden de compra simulada | Usuario | Compra validando el saldo (RN-01). | Simulador |
| **CU-05** | Ejecutar orden de venta simulada | Usuario | Venta validando los títulos poseídos (RN-02). | Simulador |
| **CU-06** | Consultar cartera y P&L | Usuario | Posiciones, ganancias o pérdidas y patrimonio consolidado (RN-04). | Simulador, Panel Principal |
| **CU-07** | Consultar historial de operaciones | Usuario | Registro cronológico de compras y ventas. | Simulador |
| **CU-08** | Realizar lección y test | Usuario | Lectura, test de evaluación y corrección (RN-05, RN-08). | Educación |
| **CU-09** | Consultar directorio y redirigir a broker | Usuario | Comparativa de brokers y salida a su web oficial (RN-06). | Brokers |
| **CU-10** | Definir presupuesto mensual | Usuario | Crear, ajustar o quitar límites por categoría y mes (C-27). | Presupuestos |
| **CU-11** | Registrar gasto y evaluar umbral | Usuario | Anotar un gasto y avisar al 80 % y al 100 % (RN-03). | Presupuestos |
| **CU-12** | Administrar contenidos y divisas | Administrador | Tipos de cambio, precios, brokers, lecciones y preguntas (C-29). | Panel de administración |
| **CU-13** | Reiniciar simulador | Usuario | Volver a 10.000 borrando cartera e historial (nuevo, C-14). | Configuración |

---

## 2. Especificación de los casos de uso principales

### CU-04 · Ejecutar orden de compra simulada
- **Precondiciones:** el usuario ha iniciado sesión y el activo existe en el catálogo.
- **Flujo principal:**
  1. El usuario abre el simulador y elige un activo del catálogo.
  2. Introduce la cantidad de títulos; se admiten hasta 4 decimales (C-24).
  3. El sistema muestra el importe en la divisa del activo y convertido a la divisa base, junto con el saldo resultante.
  4. El usuario confirma la orden.
  5. En una transacción, el sistema lee el saldo bloqueado y comprueba RN-01. Después descuenta el coste, crea la posición o recalcula el precio medio de compra y registra la operación.
  6. Muestra la confirmación con el importe.
- **Flujos alternativos:**
  - **FA-01:** al cambiar la cantidad, el importe se recalcula al momento.
  - **FA-02:** «Cancelar» abandona la orden sin cambios.
- **Excepciones:**
  - **E-01 (RN-01):** si el saldo es insuficiente, el botón de confirmar se desactiva y se explica el motivo; el servicio vuelve a comprobarlo.
  - **E-02:** si falla la base de datos, se hace *rollback* y no cambia nada.
- **Postcondiciones:** el saldo disminuye, la cartera refleja los títulos y la operación queda en el historial.

### CU-08 · Realizar lección y test
- **Precondiciones:** el usuario ha iniciado sesión y la lección está desbloqueada (RN-08).
- **Flujo principal:**
  1. El usuario elige una lección disponible en su ruta de aprendizaje.
  2. El sistema muestra el contenido.
  3. El usuario inicia el test.
  4. El usuario responde todas las preguntas (A–D) y lo envía.
  5. El sistema calcula la nota como porcentaje de aciertos.
  6. Con un 70 % o más (RN-05), la lección queda superada y se guarda la nota. Si ya tenía una mejor, se conserva esa (C-10).
  7. Se muestran la nota, la corrección de cada pregunta y, si procede, el nivel desbloqueado.
- **Flujo alternativo FA-01:** si se sale sin enviar, no se registra nada.
- **Excepción E-01 (RN-05):** con menos del 70 %, la lección no se supera, el medidor no sube y se invita a repasar y reintentar.
- **Postcondiciones:** la nota y el estado quedan en `progresos_lecciones`.

### CU-09 · Consultar directorio y redirigir a broker
- **Flujo principal:**
  1. El usuario abre el directorio.
  2. Filtra por sus preferencias.
  3. Pulsa «Abrir cuenta real».
  4. El sistema muestra el aviso de RN-06.
  5. El usuario confirma.
  6. Se abre la web oficial en el navegador del sistema.
- **Flujo alternativo FA-01:** si se cancela el aviso, el usuario sigue en la aplicación.
- **Excepción E-01:** si el enlace falta o no es válido (solo se admite `http`/`https` con dominio), se informa de que no está disponible temporalmente.
- **Postcondiciones:** no se procesa ni se guarda ningún dato bancario.

### CU-11 · Registrar gasto y evaluar umbral
- **Precondición:** existe un presupuesto para esa categoría en el mes del gasto (C-09).
- **Flujo principal:**
  1. El usuario introduce el importe, la categoría, la fecha (del mes elegido y no futura) y, opcionalmente, un concepto.
  2. Confirma el registro.
  3. El sistema guarda el gasto.
  4. Recalcula lo acumulado en esa categoría y mes.
  5. Si queda por debajo del 80 %, informa de lo que queda disponible.
- **Flujos alternativos:**
  - **FA-01:** del 80 % al 100 % sin llegar, la categoría pasa a «En Alerta» y se muestra un aviso.
  - **FA-02:** al alcanzar o superar el 100 %, pasa a «Presupuesto Excedido» y aparece una alerta de prioridad alta (ventana de aviso).
- **Excepción E-01:** un importe negativo, nulo, con formato incorrecto o con más de 2 decimales se rechaza y el campo se marca en rojo.
- **Postcondiciones:** el gasto queda guardado y las barras de progreso se actualizan.
