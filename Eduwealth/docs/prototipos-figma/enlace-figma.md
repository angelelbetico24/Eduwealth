# 🎨 Prototipos de Interfaz de Usuario (UI/UX) — EduWealth

**Proyecto:** EduWealth  
**Herramienta de diseño:** Figma / Figma AI  
**Resolución objetivo:** 1440 x 900 px (Escritorio / Web Desktop)  

---

## 1. Acceso al Prototipo Interactivo

El prototipo completo e interactivo del proyecto está disponible en la plataforma Figma a través del siguiente enlace oficial:

🔗 **[Ver Prototipo Interactivo en Figma](https://www.figma.com/design/VGJHeIRnHUelObFJ5q2S82/Dise%C3%B1o-principal-EduWealth?node-id=0-1&m=dev&t=ycwvS6MmvILs7ClQ-1)**

---

## 2. Pantallas Prototipadas y Componentes

El diseño visual abarca las 6 vistas principales de la aplicación, manteniendo una coherencia estética moderna, paleta de colores limpia (tonos azules, grises y verdes de acento) e interfaz íntegramente en español:

1. **Panel Principal (Dashboard General):**
   * Vista consolidada con resumen del saldo ficticio (RN-01), gráficos de rendimiento temporal y métricas rápidas de presupuesto.
   * Menú lateral de navegación persistente con estado activo (*active state*).

2. **Simulador de Inversiones (Paper Trading - CU-04 / CU-05):**
   * Cartera de activos en propiedad, buscador de acciones/ETFs y formulario para simular órdenes de compra/venta.
   * Mapeo visual del capital virtual de 10.000,00 € y botón de restablecimiento de saldo.

3. **Presupuestos y Control de Gastos (CU-08 / CU-09):**
   * Tarjetas de categorías con barras de progreso asociadas a los umbrales de alerta.
   * Indicadores de estado: **Verde** (Normal <80%), **Amarillo** (Advertencia ≥80% - RN-02) y **Rojo** (Exceso ≥100% - RN-03).

4. **Módulo de Educación Financiera (CU-10):**
   * Listado de lecciones ordenadas por nivel de dificultad (Principiante, Intermedio, Avanzado).
   * Barras de progreso de aprendizaje requeridas para habilitar la operativa avanzada del simulador (RN-05).

5. **Directorio de Brokers Regulados (CU-11):**
   * Catálogo comparativo de entidades financieras con filtros de comisiones y regulación.
   * Banner informativo sobre la política de no procesamiento bancario real (RN-06) y botones de derivación a webs oficiales.

6. **Configuración de Perfil y Preferencias (CU-03):**
   * Gestión de datos de usuario, cambio de contraseña y selector de divisa base de referencia (EUR, USD, GBP - RN-04).

---

## 3. Metodología de Diseño con Figma AI

Para la creación de estos prototipos se utilizaron instrucciones estandarizadas (*prompts*) en inglés solicitando la maquetación de pantallas con textos en español:

* **Estructura base:** *Sidebar navigation* fija a la izquierda + *Top Header* con perfil de usuario + Área central de trabajo (*Dashboard grid*).
* **Consistencia visual:** Sistema de componentes (*Design System*) con botones primarios/secundarios, tablas de datos y tarjetas (*Cards*) reutilizables.