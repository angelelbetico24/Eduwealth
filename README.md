# EduWealth

EduWealth es un simulador financiero interactivo y plataforma de educación patrimonial para el aprendizaje práctico de inversiones, gestión presupuestaria y derivación a plataformas financieras reguladas.

## Estructura del Repositorio

El proyecto sigue una organización modular que separa el código fuente, la documentación técnica y los artefactos de diseño:

```text
eduwealth/
├── README.md                   # Presentación general del proyecto
├── LICENSE                     # Licencia del proyecto (MIT)
├── .gitignore                  # Archivos excluidos del control de versiones
├── pom.xml                     # Configuración de dependencias Maven
│
├── docs/                       # Documentación técnica y de diseño
│   ├── analisis-diseno.md      # Resumen ejecutivo y memoria del proyecto
│   ├── requisitos.md           # Requisitos funcionales (RF), no funcionales (RNF) y reglas (RN)
│   ├── casos-uso.md            # Desglose de los 12 casos de uso y especificaciones detalladas
│   ├── modelo-datos/           # Script SQL, esquema E/R y diccionario de datos
│   │   ├── esquema-er.png
│   │   └── script-base-datos.sql
│   ├── arquitectura-clases/    # Diagrama de clases UML y arquitectura en 3 capas
│   │   └── UML-clases.png
│   └── prototipos-figma/       # Enlaces y maquetas interactivas de Figma (UI/UX)
│       └── enlace-figma.md
│
└── src/                        # Código fuente de la aplicación
    ├── main/
    │   ├── java/com/eduwealth/
    │   │   ├── controller/     # Controladores JavaFX
    │   │   ├── model/          # Entidades (POJOs)
    │   │   ├── dao/            # Capa de acceso a datos (JDBC / MySQL)
    │   │   ├── service/        # Lógica de negocio y reglas (RN-01 a RN-06)
    │   │   └── MainApp.java    # Punto de entrada de la aplicación
    │   └── resources/
    │       ├── fxml/           # Vistas en formato FXML
    │       └── css/            # Estilos visuales para JavaFX
    └── test/                   # Pruebas unitarias
```

## Acerca del Proyecto

EduWealth facilita el aprendizaje financiero mediante un entorno seguro (Paper Trading) con dinero ficticio, un gestor presupuestario con alertas dinámicas, módulos e-learning evaluables y un directorio comparativo de brokers regulados.

## Documentación Técnica (/docs)

Puedes consultar los detalles de diseño e ingeniería de software directamente en los siguientes documentos:

- **Requisitos y Reglas de Negocio:** Especificación de RF-01 a RF-08, RNF-01 a RNF-04 y reglas RN-01 a RN-06.
- **Casos de Uso:** Desglose de los 12 casos de uso oficiales y las 4 especificaciones detalladas (CU-04, CU-08, CU-09, CU-11).
- **Modelo de Datos:** Diagrama Entidad-Relación y script de base de datos MySQL.
- **Arquitectura y Clases:** Diagrama UML de clases y justificación de la arquitectura en 3 capas.
- **Prototipo UI/UX en Figma:** Maqueta visual interactiva de las pantallas principales.

## Tecnologías Utilizadas

- **Lenguaje:** Java 17+
- **Interfaz Gráfica (GUI):** JavaFX (FXML + CSS) / Figma (UI/UX)
- **Arquitectura:** 3 Capas (Presentación, Servicio, Persistencia DAO)
- **Base de Datos:** MySQL Server 8.0+
- **Persistencia:** JDBC (Java Database Connectivity)
- **Gestor de Proyecto:** Apache Maven
- **Diagramación UML:** PlantUML

## Instalación y Configuración

### Requisitos Previos

- JDK 17 o superior.
- MySQL Server 8.0+ en ejecución.
- Maven 3.8+ instalado.

### Pasos de Ejecución

1. **Clonar el repositorio:**

```bash
   git clone https://github.com/angelelbetico24/eduwealth.git
   cd eduwealth
```

2. **Inicializar la Base de Datos:** Crea una base de datos local llamada `eduwealth_db` y ejecuta el script disponible en `docs/modelo-datos/script-base-datos.sql`.

3. **Configurar Credenciales:** Ajusta la URL, usuario y contraseña de conexión MySQL en la clase `ConexionBD.java` o en el archivo de propiedades correspondiente.

4. **Compilar y Ejecutar:**

```bash
   mvn clean javafx:run
```

## Licencia

Este proyecto está distribuido bajo la licencia MIT. Consulta el archivo `LICENSE` para más información.