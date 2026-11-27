# EduWealth

Aplicación de escritorio de **educación financiera**. Permite aprender a invertir y a gestionar el dinero sin riesgo: un simulador de bolsa con dinero ficticio, presupuestos mensuales con alertas, lecciones con test y un directorio comparativo de brokers regulados.

> Proyecto académico: DAIA 2026/27, Proyecto Integral II. Autor: Ángel Muñoz López.

## Funcionalidades

| Módulo | Qué permite hacer |
|---|---|
| **Registro e inicio de sesión** | Crear una cuenta (contraseña cifrada con BCrypt) con una divisa base y 10.000 de saldo virtual. Hay dos roles: usuario y administrador. |
| **Panel principal** | Resumen del patrimonio virtual, la rentabilidad, el estado del presupuesto del mes y el progreso educativo. |
| **Simulador de inversiones** | Catálogo de acciones, ETF y criptomonedas con búsqueda y filtros. Compra y venta al precio actual con validación de saldo y de títulos, cartera con precio medio y P&L, e historial de operaciones. Los precios se mueven solos un poco cada día. |
| **Multidivisa** | Cada usuario elige su divisa base (EUR, USD o GBP) y los importes se convierten automáticamente. |
| **Presupuestos** | Límites de gasto por categoría y mes, registro de gastos y alertas al llegar al 80 % y al 100 % del límite. |
| **Educación** | Ruta de 6 lecciones en tres niveles, cada una con un test (se aprueba con un 70 %). Un nivel se desbloquea al completar el anterior, y un medidor indica la preparación para invertir con dinero real. |
| **Brokers** | Comparativa de plataformas reguladas con filtros y enlace a su web oficial. La aplicación nunca pide datos bancarios. |
| **Configuración** | Cambiar el nombre, la contraseña y la divisa base (convierte el saldo y los presupuestos), y reiniciar el simulador. |
| **Administración** | Solo para el rol ADMIN: tipos de cambio, precios, simulación de un día de mercado, y gestión de brokers, lecciones y preguntas. |

## Tecnologías

- **Java 21** y **JavaFX 21** (FXML + CSS)
- **MySQL 8.0** accedido con **JDBC** (mysql-connector-j), con transacciones para las operaciones que modifican varias tablas
- **jBCrypt** para las contraseñas
- **JUnit 5** para las pruebas unitarias
- **Maven**, mediante el wrapper `mvnw`, por lo que no hace falta instalarlo
- **Arquitectura en 3 capas**: controladores JavaFX → servicios (reglas de negocio) → DAO (JDBC)
- Desarrollado en **Visual Studio Code**; los prototipos de la interfaz están en Figma

## Requisitos previos

- **JDK 21 o superior.** La orden `java -version` debe funcionar, o la variable `JAVA_HOME` debe apuntar al JDK.
- **MySQL Server 8.0.16 o superior** en ejecución. Se recomienda MySQL Workbench para ejecutar el script.
- Git, para clonar el repositorio.

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/angelelbetico24/Eduwealth.git
cd Eduwealth/Eduwealth
```

Todas las órdenes siguientes se ejecutan dentro de `Eduwealth/Eduwealth/`, la carpeta que contiene `pom.xml`.

### 2. Crear la base de datos

Abre MySQL Workbench con el usuario `root` y ejecuta el script completo:

```
docs/modelo-datos/script-base-datos.sql
```

El script crea la base `eduwealth_db` y sus 12 tablas, y carga datos de prueba: divisas, activos, lecciones con preguntas, brokers, categorías de gasto y dos usuarios. Ten en cuenta que **borra y recrea** las tablas, así que al volver a ejecutarlo todo queda como al principio.

### 3. Crear el usuario de la aplicación

La aplicación no se conecta como `root`. Para crear un usuario con permisos solo sobre su base de datos, ejecuta como `root` lo siguiente (elige tu propia contraseña):

```sql
CREATE USER 'eduwealth'@'localhost' IDENTIFIED BY 'tu_contraseña';
GRANT SELECT, INSERT, UPDATE, DELETE ON eduwealth_db.* TO 'eduwealth'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Configurar la conexión

Copia la plantilla y escribe en la copia la contraseña del paso anterior:

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

```properties
db.url=jdbc:mysql://localhost:3306/eduwealth_db?serverTimezone=Europe/Madrid
db.user=eduwealth
db.password=tu_contraseña
```

El archivo `db.properties` está en `.gitignore`, así que las credenciales nunca se suben al repositorio.

### 5. Ejecutar la aplicación

```bash
./mvnw javafx:run          # Linux / macOS / Git Bash
mvnw.cmd javafx:run        # Windows (CMD o PowerShell)
```

La primera vez, el wrapper descarga Maven y las dependencias, y puede tardar unos minutos. Desde VS Code también se puede arrancar con la configuración **EduWealth** de *Ejecutar y depurar*, que lanza la clase `com.eduwealth.Launcher`.

Si no conecta, este programa comprueba la conexión y las tablas por consola, sin mostrar la contraseña:

```bash
./mvnw -q compile exec:java -Dexec.mainClass=com.eduwealth.util.DiagnosticoBD
```

### Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Usuario | `demo@eduwealth.com` | `demo1234` |
| Administrador | `admin@eduwealth.com` | `admin1234` |

El usuario demo ya tiene operaciones, presupuestos del mes en curso y progreso en las lecciones. También puedes registrar una cuenta nueva desde la pantalla de inicio.

## Pruebas

```bash
./mvnw test
```

Hay 71 pruebas unitarias de los servicios y las reglas de negocio: compra y venta, conversión de divisas, umbrales de presupuesto, corrección de tests, desbloqueo de niveles, administración, etc. Usan DAO falsos en memoria, así que **no necesitan MySQL**. Los informes se guardan en `target/surefire-reports/`.

## Estructura del repositorio

```text
Eduwealth/                         repositorio
├── README.md
├── LICENSE.md
└── Eduwealth/                     proyecto Maven
    ├── pom.xml, mvnw, mvnw.cmd
    ├── docs/
    │   ├── analisis-diseno.md     resumen del análisis y diseño
    │   ├── requisitos.md          RF, RNF, reglas de negocio y matriz de trazabilidad
    │   ├── casos-uso.md           CU-01 a CU-13 y especificaciones detalladas
    │   ├── cambios/
    │   │   └── cambios-v1.1.md    revisión del diseño: cada cambio con su problema y su solución
    │   ├── modelo-datos/
    │   │   ├── script-base-datos.sql   esquema completo con datos de prueba
    │   │   ├── esquema-er.png
    │   │   └── migraciones/            cambios de esquema sobre una base ya creada
    │   ├── arquitectura-clases/   diagrama UML de clases
    │   └── prototipos-figma/      enlace a los prototipos
    └── src/
        ├── main/java/com/eduwealth/
        │   ├── Launcher.java, MainApp.java
        │   ├── model/             entidades y enumerados
        │   ├── dao/               interfaces DAO y transacciones
        │   │   └── jdbc/          implementación JDBC con MySQL
        │   ├── service/           reglas de negocio (RN-01 a RN-08)
        │   ├── controller/        controladores JavaFX
        │   └── util/              conexión, sesión, navegación y formato
        ├── main/resources/
        │   ├── fxml/              vistas
        │   ├── css/eduwealth.css  estilos
        │   └── db.properties.example
        └── test/java/             pruebas unitarias (JUnit 5)
```

## Documentación

El diseño parte del *Documento de Análisis y Diseño v1.0*. Los errores encontrados al implementarlo y las decisiones tomadas para corregirlos (C-01 a C-29) están en [`docs/cambios/cambios-v1.1.md`](Eduwealth/docs/cambios/cambios-v1.1.md), y son la base de la versión 2 del documento.

## Licencia

Distribuido bajo la licencia MIT. Consulta [`LICENSE.md`](LICENSE.md).
