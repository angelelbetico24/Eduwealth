# 📑 Documento de Análisis y Diseño v1.0 — EduWealth

**Proyecto:** EduWealth — Plataforma de Simulación Financiera y Control de Gastos  
**Fecha:** Octubre 2026  
**Tecnologías principales:** Java 17, JavaFX, JDBC, MySQL, Figma, Git/GitHub  

---

## 1. Introducción y Propósito del Sistema

**EduWealth** es una aplicación de escritorio diseñada para fomentar la educación financiera, el control de presupuestos personales y la simulación práctica de inversiones (*Paper Trading*) sin riesgo económico real. 

El propósito de este documento es detallar la arquitectura del sistema, el modelo funcional (casos de uso), el diseño relacional de la base de datos, las especificaciones de interfaz gráfica (UI/UX) y las decisiones tecnológicas adoptadas durante la fase de análisis y diseño[cite: 1].

---

## 2. Reglas de Negocio (RN)

El comportamiento y la lógica del sistema están estrictamente delimitados por las siguientes reglas de negocio:

* **RN-01 (Capital Virtual Inicial):** Al registrarse un usuario, el sistema asigna automáticamente un saldo ficticio inicial de **10.000,00 €**.
* **RN-02 (Umbral de Advertencia de Presupuesto):** Se genera una alerta preventiva de estado cuando los gastos de una categoría alcanzan el **80%** del límite configurado.
* **RN-03 (Umbral de Exceso de Presupuesto):** Se notifica de manera crítica cuando los gastos acumulados en una categoría alcanzan o superan el **100%** del límite asignado.
* **RN-04 (Consolidación Multidivisa):** El sistema permite seleccionar una divisa base de preferencia (EUR, USD, GBP), convirtiendo de forma visual los valores de la cartera y presupuestos a dicha moneda.
* **RN-05 (Requisito Educativo):** Para habilitar la operativa avanzada en el simulador de inversiones, el usuario debe completar el nivel básico del módulo de formación.
* **RN-06 (No Procesamiento Bancario Real):** EduWealth **no procesa dinero ni credenciales bancarias reales**. La vinculación con brokers se realiza mediante derivación a sitios web oficiales externos.

---

## 3. Arquitectura del Sistema (3 Capas)

La aplicación sigue el patrón arquitectónico en **3 Capas (Layered Architecture)** para garantizar la mantenibilidad, escalabilidad y la separación de responsabilidades: