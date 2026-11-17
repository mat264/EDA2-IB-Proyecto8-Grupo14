# EPN — Estructuras de Datos y Algoritmos II
## Proyecto 8 (Grupo 14): Sistema de Recomendación Colaborativa Basado en Grafos Bipartitos y Búsqueda en Anchura (BFS)

Aplicación de escritorio interactiva desarrollada en **Java 17+** y **JavaFX 21.0.2**, gestionada con **Apache Maven** y validada mediante pruebas unitarias automatizadas en **JUnit 5 (`5.10.2`)**. Diseñada de forma 100% portable para compilar y ejecutarse en **Linux, Windows y macOS** sin depender de rutas absolutas.

> 📄 **Documentación Técnica para el Informe IEEE y Defensa Oral:**  
> Consulte el documento detallado en [`docs/DOCUMENTACION_TECNICA_INFORME.md`](docs/DOCUMENTACION_TECNICA_INFORME.md).

---

## 1. Requisitos del Sistema
- **Java Development Kit (JDK):** Versión **17 o superior** (compatible con JDK 17, 21 y 25+).
- **Apache Maven:** Versión **3.8+** (o mediante el script Maven Wrapper `./mvnw` / `mvnw.cmd`).
- **Sistema Operativo:** Linux, Windows o macOS (las dependencias nativas de OpenJFX se resuelven automáticamente según el sistema operativo).

---

## 2. Comandos Exactos de Compilación, Pruebas y Ejecución

Abra una terminal en la carpeta raíz del proyecto (`ProyectoIB-Grupo14/`) y ejecute:

### Usando Apache Maven instalado en el sistema:
```bash
# 1. Limpiar y compilar el código fuente y empaquetar los recursos CSV/CSS
mvn clean compile

# 2. Ejecutar los 5 escenarios de pruebas unitarias automatizadas (JUnit 5)
mvn test

# 3. Iniciar la aplicación gráfica JavaFX
mvn javafx:run
```

### Usando Maven Wrapper (si prefiere no instalar Maven globalmente):
```bash
# En Linux / macOS:
./mvnw clean compile
./mvnw test
./mvnw javafx:run

# En Windows (PowerShell / CMD):
mvnw.cmd clean compile
mvnw.cmd test
mvnw.cmd javafx:run
```

> **Ejecución directa desde IDEs (IntelliJ IDEA, VS Code, Eclipse):**  
> Además de `mvn javafx:run`, puede ejecutar directamente la clase envoltorio **`com.epn.eda2.Launcher`** (*Run Java*), la cual carga el entorno de JavaFX sin requerir argumentos `--module-path` manuales y evita el error `JavaFX runtime components are missing`.

---

## 3. Formato de los Archivos CSV de Entrada

El sistema carga por defecto los datasets incluidos en `src/main/resources/data/` (mediante `getResourceAsStream()`) o en la carpeta relativa `data/`, y además permite cargar archivos CSV externos en vivo desde el botón **"📂 Cargar CSV Externos..."** de la interfaz.

### 3.1 Archivo de Catálogo de Películas (`movies.csv` / `synthetic_movies.csv`)
Formato separado por comas con encabezado `movieId,title,genres` (soporta títulos entre comillas que contengan comas):
```csv
movieId,title,genres
1,The Matrix (1999),Action|Sci-Fi
2,Inception (2010),Action|Sci-Fi|Thriller
3,Interstellar (2014),Adventure|Drama|Sci-Fi
```
- **`movieId`** (`int`): Identificador único de la película ($v \in V$).
- **`title`** (`String`): Título y año de estreno.
- **`genres`** (`String`): Géneros separados por tubería (`|`).

### 3.2 Archivo de Calificaciones (`ratings.csv` / `synthetic_ratings.csv`)
Formato separado por comas con encabezado `userId,movieId,rating` (también soporta una cuarta columna opcional `timestamp` estándar de MovieLens):
```csv
userId,movieId,rating
1,1,5.0
1,2,4.5
4,7,3.0
```
- **`userId`** (`int`): Identificador único del usuario ($u \in U$).
- **`movieId`** (`int`): Identificador de la película calificada ($v \in V$).
- **`rating`** (`double`): Calificación numérica. **El grafo bipartito filtra e inserta como aristas únicamente las interacciones positivas con `rating >= 4.0`**.

---

## 4. Estructura Completa del Proyecto (`ProyectoIB-Grupo14`)

```text
ProyectoIB-Grupo14/
├── pom.xml                                      # Configuración Maven (Java 17, OpenJFX 21.0.2, JUnit 5.10.2)
├── README.md                                    # Manual de instalación, formato CSV y ejecución rápida
├── docs/
│   └── DOCUMENTACION_TECNICA_INFORME.md         # Explicación de arquitectura, Big-O, NP-Completos y guía IEEE
├── data/
│   ├── movies.csv                               # Catálogo MovieLens en ruta relativa local
│   └── ratings.csv                              # Calificaciones MovieLens en ruta relativa local
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── epn/
    │   │           └── eda2/
    │   │               ├── MainApp.java                 # Aplicación principal JavaFX
    │   │               ├── Launcher.java                # Punto de entrada libre de errores de módulos JavaFX
    │   │               ├── model/
    │   │               │   ├── Movie.java               # Entidad Película (v in V)
    │   │               │   ├── User.java                # Entidad Usuario (u in U)
    │   │               │   ├── Rating.java              # Registro de calificación (userId, movieId, rating)
    │   │               │   ├── Recommendation.java      # Resultado Nivel 3 (puntaje, caminos, Jaccard, explicación)
    │   │               │   └── BipartiteGraph.java      # Grafo Bipartito con HashMap + HashSet O(1)
    │   │               ├── algorithm/
    │   │               │   └── BFSRecommender.java      # Motor BFS de 3 saltos, similitud de Jaccard y Top-K
    │   │               ├── metrics/
    │   │               │   ├── BenchmarkResult.java     # Contenedor de métricas experimentales
    │   │               │   └── Evaluator.java           # Validación 80/20 Train/Test (Precision@K, Recall@K, RAM)
    │   │               ├── ui/
    │   │               │   ├── MainView.java            # Pestaña 1: Tablas, controles reactivos y panel Benchmark
    │   │               │   └── GraphVisualizerPane.java # Pestaña 2: Visualizador BFS animado paso a paso
    │   │               └── util/
    │   │                   └── CsvDatasetLoader.java    # Lector CSV portable (Classpath + Rutas relativas + FileChooser)
    │   └── resources/
    │       ├── data/
    │       │   ├── synthetic_movies.csv                 # Dataset sintético embebido (8 películas)
    │       │   ├── synthetic_ratings.csv                # Dataset sintético embebido (6 usuarios)
    │       │   ├── movies.csv                           # Dataset MovieLens embebido de respaldo
    │       │   └── ratings.csv                          # Calificaciones MovieLens embebidas de respaldo
    │       └── styles/
    │           └── projector.css                        # Hoja de estilos optimizada para proyector (14px-17px)
    └── test/
        └── java/
            └── com/
                └── epn/
                    └── eda2/
                        └── RecommenderTest.java         # Suite JUnit 5 con los 5 escenarios de prueba
```

---

## 5. Resumen de Funcionalidades en la Interfaz Gráfica

1. **Pestaña 1 — Sistema de Recomendación y Métricas:**
   - Selección en vivo entre el dataset **Sintético Demo (6 usuarios, 8 películas)**, **MovieLens (`movies.csv` / `ratings.csv`)** o archivos CSV externos.
   - Sincronización automática al cambiar el **Usuario Objetivo** o el parámetro **`Top-K`**.
   - Tabla de **Películas Vistas (Nivel 1)** y tabla de **Películas Candidatas Recomendadas (Nivel 3)** con trazabilidad completa sin texto truncado y `Tooltips` informativos.
   - Panel de **Evaluación y Benchmarks (80/20)** que calcula en vivo `Precision@K`, `Recall@K`, tiempo de construcción del grafo (ms), tiempo promedio de recomendación (ms) y memoria utilizada (MB).
2. **Pestaña 2 — Visualizador Interactivo del Grafo (Defensa Oral):**
   - Representación en 4 columnas verticales (`Paso 1: Usuario Objetivo`, `Paso 2: Películas Vistas`, `Paso 3: Usuarios Similares`, `Paso 4: Películas Candidatas`).
   - Botones **"Paso Siguiente (BFS) ▶"** y **"Reiniciar ↺"** con animaciones suaves (`FadeTransition` + `ScaleTransition`) y consola inferior con la explicación algorítmica y complejidad Big-O simplificada en 3 líneas.
