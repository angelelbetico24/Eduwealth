# Documento de Análisis y Diseño v1.0 — EduWealth

- **Proyecto:** EduWealth — Plataforma de Simulación Financiera y Control de Gastos
- **Fecha:** Octubre 2026
- **Tecnologías principales:** Java 17, JavaFX, JDBC, MySQL, Figma, Git/GitHub

---

## 1. Introducción y Propósito del Sistema

**EduWealth** es una aplicación de escritorio diseñada para fomentar la educación financiera, el control de presupuestos personales y la simulación práctica de inversiones (*Paper Trading*) sin riesgo económico real.

El propósito de este documento es detallar la arquitectura del sistema, el modelo funcional (casos de uso), el diseño relacional de la base de datos, las especificaciones de interfaz gráfica (UI/UX) y las decisiones tecnológicas adoptadas durante la fase de análisis y diseño.

---

## 2. Reglas de Negocio (RN)

El comportamiento y la lógica del sistema están estrictamente delimitados por las siguientes reglas de negocio:

- **RN-01 (Capital Virtual Inicial):** Un usuario no podrá realizar una orden de compra en el simulador si el coste total supera el saldo ficticio disponible (inicialmente 10.000,00 €).
- **RN-02 (Límite de Venta Simulada):** Un usuario no podrá vender más títulos o participaciones de un activo de las que posee actualmente en su cartera virtual.
- **RN-03 (Alerta por Umbrales de Presupuesto):** Cuando los gastos acumulados en una categoría alcancen o superen el 80% se marca como "En Alerta", y al 100% como "Presupuesto Excedido".
- **RN-04 (Conversión Obligatoria a Moneda Base):** Todas las transacciones o activos cotizados en otra divisa se convierten al tipo de cambio registrado antes de sumarse al patrimonio neto consolidado.
- **RN-05 (Bloqueo de Recompensa de Lección):** Un test solo se considera aprobado si el usuario obtiene una calificación igual o superior al 70% de aciertos.
- **RN-06 (Restricción de Operaciones Reales):** La aplicación no procesará ni almacenará datos bancarios reales. Toda operación real deriva al usuario al sitio oficial del broker externo mediante un enlace explícito.

---

## 3. Arquitectura del Sistema (3 Capas)

La aplicación sigue el patrón arquitectónico en **3 Capas (Layered Architecture)** para garantizar la mantenibilidad, escalabilidad y la separación de responsabilidades:

```text
+-------------------------------------------------------+
|   Capa de Presentación (UI)                           |
|   - Vistas FXML / CSS (JavaFX)                        |
|   - Controladores de Interfaz (package controller)    |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|   Capa de Lógica de Negocio (Service)                 |
|   - Servicios del Sistema (package service)           |
|   - Validación de RN-01 a RN-06                       |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|   Capa de Persistencia y Datos (DAO / Database)       |
|   - Objetos de Acceso a Datos (package dao - JDBC)    |
|   - Base de Datos Relacional MySQL                    |
+-------------------------------------------------------+
```

1. **Capa de Presentación:** Gestión de la interfaz con JavaFX. Captura los eventos del usuario y actualiza los componentes visuales.
2. **Capa de Lógica de Negocio:** Aplica las validaciones del sistema, cálculos de rentabilidad/gastos y control de umbrales.
3. **Capa de Persistencia:** Ejecuta consultas y actualizaciones mediante **JDBC** sobre la base de datos **MySQL**.

---

## 4. Estructura de Paquetes en Java

El código fuente en `src/main/java/com/eduwealth/` está organizado de la siguiente manera:

- `com.eduwealth.model`: Entidades POJO (`Usuario`, `Activo`, `PosicionCartera`, `Gasto`, `CategoriaGasto`, `Leccion`, `Broker`).
- `com.eduwealth.dao`: Interfaces e implementaciones JDBC para el acceso a datos.
- `com.eduwealth.service`: Clases de negocio (`SimuladorService`, `PresupuestoService`, `EducacionService`).
- `com.eduwealth.controller`: Controladores FXML de JavaFX.

---

## 5. Documentación Adjunta

Para profundizar en los detalles técnicos del proyecto, consulta los documentos específicos dentro de la carpeta `/docs`:

- **Requisitos del Sistema ([`/docs/requisitos.md`](requisitos.md)):** Catálogo de Requisitos Funcionales (RF) y No Funcionales (RNF).
- **Casos de Uso y Especificaciones ([`/docs/casos-uso.md`](casos-uso.md)):** Tabla de casos de uso y especificaciones detalladas.
- **Modelo de Datos y Script SQL ([`/docs/modelo-datos/`](modelo-datos/)):** Diagrama Entidad-Relación, diccionario de datos y scripts de creación de tablas en MySQL.
- **Prototipos de UI/UX en Figma ([`/docs/prototipos-figma/`](prototipos-figma/)):** Enlaces, capturas y pantallas navegables de la aplicación.