# Catálogo de requisitos — EduWealth

Requisitos del **Documento de Análisis y Diseño v1.0** (apartados 6.4, 6.5 y 6.6) con los cambios de la revisión **v1.1** aplicados. El detalle y la justificación de cada cambio (C-xx) están en [`cambios/cambios-v1.1.md`](cambios/cambios-v1.1.md).

---

## 1. Requisitos funcionales (RF)

### Usuarios y sesión
| ID | Requisito |
|---|---|
| **RF-01** | **Registro de usuario:** nombre, correo electrónico, contraseña y moneda de referencia principal (EUR, USD, GBP). |
| **RF-02** | **Inicio de sesión:** acceso con correo electrónico y contraseña. |
| **RF-03** | **Configuración de perfil:** cambiar el nombre, la moneda de referencia base (con conversión de saldo, presupuestos y gastos, C-28) y la contraseña. |

### Simulador de inversiones (Paper Trading)
| ID | Requisito |
|---|---|
| **RF-04** | **Saldo ficticio:** al crear la cuenta se asignan 10.000 en la divisa base del usuario (C-07). |
| **RF-05** | **Compra ficticia:** el usuario indica el activo (ticker) y la cantidad de títulos; el precio es siempre el actual del catálogo (C-15). |
| **RF-06** | **Venta ficticia:** venta parcial o total; actualiza el saldo y registra la plusvalía o minusvalía realizada (C-04). |
| **RF-07** | **Cartera y rendimiento (P&L):** composición de la cartera con precio medio de compra, precio actual y beneficio o pérdida. Los precios evolucionan con el mercado simulado (C-26). |
| **RF-08** | **Historial de operaciones:** lista de todas las compras y ventas ficticias. |
| **RF-19** | **Reinicio del simulador:** vuelve a 10.000, borra la cartera y el historial y conserva el progreso educativo y los presupuestos (C-14). |

### Multidivisa
| ID | Requisito |
|---|---|
| **RF-09** | **Divisas y tipos de cambio:** el sistema guarda las divisas con su tipo de cambio respecto al EUR, que actúa como pivote (C-06), y su fecha de actualización (C-08). |
| **RF-10** | **Consolidación en moneda principal:** el patrimonio total se muestra convertido a la divisa base del usuario (RN-04). |

### Educación financiera y brokers
| ID | Requisito |
|---|---|
| **RF-11** | **Catálogo de lecciones** por niveles: principiante, intermedio y avanzado. |
| **RF-12** | **Test de evaluación** al final de cada lección, con su nota (preguntas tipo test, C-01; se guarda la mejor nota, C-10). |
| **RF-13** | **Medidor de preparación** según los test superados y la práctica en el simulador (RN-07). |
| **RF-14** | **Directorio de brokers** regulados con filtros por comisiones, tipos de activo y nivel recomendado (C-22). |
| **RF-15** | **Derivación al broker:** enlace seguro a la web oficial, con aviso previo, cuando el usuario decide operar con dinero real. |

### Presupuestos y control de gastos
| ID | Requisito |
|---|---|
| **RF-16** | **Registro de gastos** asociados a una categoría (C-17: el módulo no registra ingresos). |
| **RF-17** | **Presupuestos por categoría:** límites mensuales de gasto (C-27). |
| **RF-18** | **Monitor de umbrales y alertas** al 80 % y al 100 % (RN-03). |

---

## 2. Requisitos no funcionales (RNF)

| ID | Requisito | Cómo se cumple |
|---|---|---|
| **RNF-01** | Contraseñas nunca en texto plano | Hash **BCrypt** (jBCrypt), C-20 |
| **RNF-02** | Persistencia relacional con integridad y transacciones | MySQL con claves foráneas, `UNIQUE` y `CHECK` (C-23); compras, ventas, reinicio, cambio de divisa, límites y test en transacción (C-25) |
| **RNF-03** | Interfaz gráfica nativa en JavaFX | JavaFX 21 con FXML y una hoja CSS |
| **RNF-04** | Simulador, lecciones y brokers a ≤ 3 clics del menú | Todos los módulos están a **1 clic** en el menú lateral |
| **RNF-05** | Consultas de cartera, presupuestos y divisas en menos de 1 s | Consultas agregadas (`LEFT JOIN` + `SUM`) y catálogos cargados una vez por pantalla |
| **RNF-06** | Arquitectura en capas preparada para módulos de IA | `controller` → `service` → `dao` → MySQL; los servicios no dependen de JavaFX ni de JDBC |

---

## 3. Reglas de negocio (RN)

| ID | Regla |
|---|---|
| **RN-01** | **Límite de compra:** no se puede comprar si el coste, convertido a la divisa base, supera el saldo ficticio disponible. |
| **RN-02** | **Límite de venta:** no se pueden vender más títulos de los que se poseen. |
| **RN-03** | **Umbrales de presupuesto:** menos del 80 %, «Dentro del límite»; del 80 % al 100 % sin llegar, «En Alerta»; 100 % o más, «Presupuesto Excedido». Los rangos no tienen huecos (C-16). |
| **RN-04** | **Conversión a la moneda base:** todo importe en otra divisa se convierte al tipo de cambio registrado antes de sumarse al patrimonio. |
| **RN-05** | **Aprobado del test:** se necesita un 70 % de aciertos o más; si no, la lección no se supera y el medidor no sube. |
| **RN-06** | **Sin operaciones reales:** la aplicación no procesa ni guarda datos bancarios; la inversión real se hace en la web oficial del broker. |
| **RN-07** | **Medidor de preparación** (nueva, C-12): `70 × (lecciones aprobadas ÷ totales) + 30 × min(operaciones ÷ 10, 1)`. Desde el 75 % el usuario está «Listo para la inversión real». |
| **RN-08** | **Desbloqueo por niveles** (nueva, C-13): las lecciones de un nivel se desbloquean al aprobar todas las del nivel anterior. |

---

## 4. Matriz de trazabilidad

| Requisito | Caso de uso | Reglas | Implementación (servicio · pantalla) |
|---|---|---|---|
| RF-01 | CU-01 | — | `AutenticacionService.registrar` · `registro.fxml` |
| RF-02 | CU-02 | — | `AutenticacionService.iniciarSesion` · `login.fxml` |
| RF-03 | CU-02 | RN-04 | `PerfilService` · `configuracion.fxml` |
| RF-04 | CU-01 | RN-01 | `AutenticacionService.SALDO_INICIAL` |
| RF-05 | CU-03, CU-04 | RN-01, RN-04 | `SimuladorService.comprar` · `simulador.fxml` |
| RF-06 | CU-05 | RN-02, RN-04 | `SimuladorService.vender` · `simulador.fxml` |
| RF-07 | CU-06 | RN-04 | `SimuladorService.resumenCartera`, `MercadoService` · `simulador.fxml`, `inicio.fxml` |
| RF-08 | CU-07 | — | `SimuladorService.historial` · `simulador.fxml` |
| RF-09 | CU-12 | — | `DivisaService`, `AdminService.actualizarTipoCambio` · `admin.fxml` |
| RF-10 | CU-06 | RN-04 | `DivisaService.convertir` · `inicio.fxml`, `simulador.fxml` |
| RF-11 | CU-08 | RN-08 | `EducacionService.rutaDeAprendizaje` · `educacion.fxml` |
| RF-12 | CU-08 | RN-05 | `EducacionService.corregirTest` · `leccion.fxml` |
| RF-13 | CU-08 | RN-07 | `EducacionService.resumen` · `educacion.fxml`, `inicio.fxml` |
| RF-14 | CU-09 | — | `BrokerService.filtrar` · `brokers.fxml` |
| RF-15 | CU-09 | RN-06 | `BrokerService.enlaceOficial` · `brokers.fxml` |
| RF-16 | CU-11 | RN-03 | `PresupuestoService.registrarGasto` · `presupuestos.fxml` |
| RF-17 | CU-10 | — | `PresupuestoService.guardarLimites` · ventana «Gestionar límites» |
| RF-18 | CU-11 | RN-03 | `PresupuestoService.evaluarUmbral` · `presupuestos.fxml`, `inicio.fxml` |
| RF-19 | CU-13 | — | `PerfilService.reiniciarSimulador` · `configuracion.fxml` |

Las reglas de negocio tienen pruebas unitarias en `src/test/java/com/eduwealth/service/`.
