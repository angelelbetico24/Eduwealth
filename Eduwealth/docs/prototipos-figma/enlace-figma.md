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
   * Patrimonio total consolidado (RN-04), presupuesto del mes con su estado (RN-03), medidor de preparación (RN-07), resumen de cartera, novedades y broker recomendado.
   * Menú lateral de navegación persistente con estado activo (*active state*).

2. **Simulador de Inversiones (Paper Trading, CU-03 a CU-07):**
   * Cartera con su P&L, catálogo con buscador y formulario de órdenes de compra y venta (RN-01, RN-02).
   * Saldo virtual de 10.000 € y brokers recomendados.

3. **Presupuestos y Control de Gastos (CU-10, CU-11):**
   * Tarjetas de categorías con barras de progreso según los umbrales de alerta.
   * Indicadores de estado (RN-03): **verde**, dentro del límite (< 80 %); **ámbar**, en alerta (≥ 80 %); **rojo**, presupuesto excedido (≥ 100 %).

4. **Módulo de Educación Financiera (CU-08):**
   * Lecciones ordenadas por nivel de dificultad (principiante, intermedio y avanzado), con bloqueo del nivel siguiente hasta superar el actual (RN-08).
   * Medidor de preparación para la inversión real (RN-07) y nota de los test (RN-05).

5. **Directorio de Brokers Regulados (CU-09):**
   * Catálogo comparativo de brokers con filtros por comisiones, tipo de activo, regulación y nivel.
   * Banner informativo sobre la política de no procesamiento bancario real (RN-06) y botones de derivación a las webs oficiales.

6. **Configuración de Perfil y Preferencias (CU-02, CU-13):**
   * Datos de usuario, cambio de contraseña y selector de divisa base (EUR, USD, GBP, RN-04).
   * Botón para reiniciar el saldo virtual a 10.000 € (RF-19).

> **Implementación:** la aplicación JavaFX reproduce estas seis pantallas y añade la vista de lección y test, la ventana «Gestionar límites» de presupuestos y el panel de administración (CU-12), que no estaban prototipados. Las diferencias de comportamiento respecto al documento v1.0 están justificadas en [`../cambios/cambios-v1.1.md`](../cambios/cambios-v1.1.md).

---

## 3. Metodología de Diseño con Figma AI

Para la creación de estos prototipos se utilizaron instrucciones estandarizadas (*prompts*) en inglés solicitando la maquetación de pantallas con textos en español:

* **Estructura base:** *Sidebar navigation* fija a la izquierda + *Top Header* con perfil de usuario + Área central de trabajo (*Dashboard grid*).
* **Consistencia visual:** Sistema de componentes (*Design System*) con botones primarios/secundarios, tablas de datos y tarjetas (*Cards*) reutilizables.