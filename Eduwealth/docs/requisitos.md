# 📋 Catálogo de Requisitos del Sistema — EduWealth

---

## 1. Requisitos Funcionales (RF)

### **Módulo 1: Autenticación y Configuración de Perfil**
* **RF-01 (Registro de Usuario):** Permitir a nuevos usuarios registrarse con nombre completo, correo electrónico y contraseña.
* **RF-02 (Inicio de Sesión):** Autenticar usuarios mediante credenciales válidas (correo y contraseña).
* **RF-03 (Configuración de Preferencias):** Permitir actualizar el perfil y seleccionar la divisa base de referencia (EUR, USD, GBP) según la regla `RN-04`.

### **Módulo 2: Simulador de Inversiones (Paper Trading)**
* **RF-04 (Consulta de Cartera y Saldo):** Mostrar el saldo virtual disponible (inicial de 10.000,00 € segun `RN-01`), valor total invertido y rentabilidad acumulada.
* **RF-05 (Ejecución de Órdenes Simuladas):** Permitir simular órdenes de compra y venta de activos (acciones/ETFs) indicando activo y cantidad.
* **RF-06 (Historial de Transacciones):** Desplegar el historial cronológico de todas las operaciones de compra y venta realizadas.
* **RF-07 (Restablecimiento de Saldo Virtual):** Permitir reiniciar el saldo virtual a 10.000,00 € previa confirmación.

### **Módulo 3: Presupuestos y Control de Gastos**
* **RF-08 (Registro de Gastos):** Permitir registrar transacciones de gasto con monto, fecha, concepto y categoría.
* **RF-09 (Gestión de Presupuestos por Categoría):** Definir límites mensuales de gasto por categoría (Alimentación, Ocio, Vivienda, etc.).
* **RF-10 (Control de Umbrales de Alerta):** Evaluar automáticamente los gastos y mostrar alertas visuales al alcanzar el 80% (`RN-02`) y el 100% (`RN-03`).

### **Módulo 4: Educación Financiera y Directorio de Brokers**
* **RF-11 (Módulo Formativo):** Ofrecer lecciones y cuestionarios de formación financiera registrando el progreso (`RN-05`).
* **RF-12 (Directorio de Brokers Regulados):** Mostrar un catálogo con información de brokers regulados, filtros de búsqueda y enlaces oficiales externos (`RN-06`).

---

## 2. Requisitos No Funcionales (RNF)

* **RNF-01 (Rendimiento):** Operaciones locales sobre la base de datos ejecutadas en un tiempo inferior a 1 segundo.
* **RNF-02 (Interfaz de Usuario y Usabilidad):** Interfaz adaptada a 1440x900px, intuitiva y con el 100% de los textos en español.
* **RNF-03 (Seguridad de Credenciales):** Contraseñas cifradas mediante algoritmos de hash seguros (BCrypt/SHA-256) en MySQL.
* **RNF-04 (Arquitectura):** Sistema construido bajo arquitectura en 3 capas (Presentación, Negocio, Persistencia) en Java 17, JavaFX y JDBC.
* **RNF-05 (Integridad de Datos):** Base de datos MySQL con restricciones de clave primaria, clave foránea y campos no nulos.
* **RNF-06 (Portabilidad):** Ejecutable en Windows, macOS y Linux con JRE 17+.

---

## 3. Matriz de Trazabilidad

| Requisito Funcional | Caso de Uso | Regla de Negocio |
| :--- | :--- | :--- |
| **RF-01** | CU-01 (Registrar Usuario) | — |
| **RF-02** | CU-02 (Iniciar Sesión) | — |
| **RF-03** | CU-03 (Gestionar Perfil) | RN-04 (Consolidación Multidivisa) |
| **RF-04** | CU-04 (Consultar Cartera Simulada) | RN-01 (Capital Virtual Inicial) |
| **RF-05** | CU-05 (Ejecutar Orden Compra/Venta) | RN-01, RN-05 (Requisito Educativo) |
| **RF-06** | CU-06 (Ver Historial de Transacciones) | — |
| **RF-07** | CU-07 (Reiniciar Saldo Virtual) | RN-01 (Capital Virtual Inicial) |
| **RF-08** | CU-08 (Registrar Gasto Personal) | — |
| **RF-09 / RF-10** | CU-09 (Configurar Presupuestos y Alertas) | RN-02 (80%), RN-03 (100%) |
| **RF-11** | CU-10 (Completar Módulo Educativo) | RN-05 (Requisito Educativo) |
| **