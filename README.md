# Re-Acción 🎮

**Primer proyecto mobile en Android Studio** — Materia: Desarrollo de APIs  
Candela Rico · 2026

---

## ¿Qué es?

Re-Acción es un juego de tiempo de reacción para Android. La idea es simple: aparece un estímulo en pantalla (un color, un número, o una palabra), y el jugador tiene que presionar el botón lo más rápido posible. Cuanto antes reacciones, más puntos ganás.

El juego tiene 3 niveles que se van complicando: en cada nivel el tiempo máximo para reaccionar se reduce, así que lo que funcionó antes deja de alcanzar. Al final de la partida se muestran las estadísticas de esa sesión y se guardan en una base de datos local para poder comparar con partidas anteriores.

---

## Cómo ejecutarlo

**Requisitos:**
- Android Studio (Hedgehog o más nuevo)
- Android SDK 24 o superior (Android 7.0+)
- Un dispositivo físico o emulador

**Pasos:**
1. Clonar el repositorio:
   ```
   git clone https://github.com/camaleonica/re-accion.git
   ```
2. Abrir Android Studio → `File → Open` → seleccionar la carpeta del proyecto
3. Esperar a que Gradle sincronice las dependencias (aparece una barra de progreso abajo)
4. Conectar un celular con depuración USB activada, o iniciar un emulador
5. Presionar el botón verde ▶ `Run`

> **Nota:** si el celular ya tenía instalada una versión anterior de la app, desinstalarla antes de volver a instalar para que los permisos se apliquen correctamente.

---

## Estructura del proyecto

```
app/src/main/
├── java/com/desafio/reaccion/
│   ├── ConfigActivity.java       ← pantalla de inicio y configuración
│   ├── GameActivity.java         ← pantalla del juego en sí
│   ├── ResultActivity.java       ← pantalla de resultados
│   ├── StatsActivity.java        ← historial de partidas
│   ├── adapter/
│   │   └── ResultadoAdapter.java ← adaptador para la lista de estadísticas
│   └── db/
│       ├── AppDatabase.java      ← instancia singleton de la base de datos
│       ├── ResultadoDAO.java     ← consultas SQL (Room)
│       └── ResultadoPartida.java ← modelo de datos de una partida
├── res/
│   ├── layout/                   ← archivos XML de cada pantalla
│   ├── drawable/                 ← píldoras de estado (OK, GAME OVER, etc.)
│   ├── font/                     ← tipografías (Unbounded, DM Mono, Playfair)
│   ├── values/                   ← colores, strings, temas
│   └── anim/                     ← animaciones de transición entre pantallas
└── AndroidManifest.xml           ← configuración general de la app
```

---

## Las pantallas, una por una

### ConfigActivity — Pantalla de inicio

Es lo primero que ve el jugador. Acá se configura la partida antes de empezar.

Tiene cuatro campos:
- **Nombre del jugador:** se guarda automáticamente con `SharedPreferences` para que la próxima vez que abras la app ya esté escrito.
- **Modo de juego:** un dropdown con cuatro opciones.
  - *Entrenamiento*: podés practicar sin que se guarden puntos ni penalizaciones.
  - *Fácil*: tiempo máximo de 20 segundos por estímulo.
  - *Medio*: 15 segundos.
  - *Difícil*: 10 segundos.
- **Cantidad de rondas:** cuántos estímulos hay por nivel (por defecto 20).
- **Tiempo máximo:** un slider que ajusta el tiempo límite para reaccionar. Cambia automáticamente según el modo elegido, pero se puede personalizar.

Cuando se presiona JUGAR, esta activity arma un `Intent` con todos los datos configurados y se los pasa a `GameActivity` como extras.

---

### GameActivity — El juego

Es el corazón de la app. Acá pasan todas las cosas.

**Cómo funciona el flujo:**

1. Arranca una cuenta regresiva 3→2→1→¡YA! para que el jugador se prepare.
2. Empieza la primera ronda: el juego espera un tiempo aleatorio entre 1 y 3 segundos y después muestra el estímulo.
3. El jugador presiona ¡REACCIONAR! lo más rápido posible.
4. Se calcula el tiempo de reacción, se suman puntos y arranca la siguiente ronda.
5. Al completar todas las rondas del nivel, el juego sube al siguiente (con tiempo reducido).
6. Al terminar los 3 niveles, se va a la pantalla de resultados.

**Los estímulos son de tres tipos:**
- **Color:** aparece el nombre de un color (VERDE, ROJO, AZUL, etc.) y el recuadro se ilumina con ese color.
- **Número:** aparece un número del 1 al 9 sobre fondo amarillo.
- **Palabra:** aparece una palabra de acción (YA!, TAP!, AHORA!, PULSA!) sobre fondo violeta.

**Los niveles aumentan la dificultad así:**
- Nivel 1: tiempo máximo configurado (ej: 20s)
- Nivel 2: 70% del tiempo del nivel 1
- Nivel 3: 50% del tiempo del nivel 1

**El puntaje se calcula así:**
```
puntos = ((tiempo_max - tiempo_reaccion) / tiempo_max * 100) * numero_de_nivel
```
Cuanto más rápido reaccionás y más avanzado está el nivel, más puntos ganás.

**Game Over ocurre cuando:**
- Se agota el tiempo sin que el jugador reaccione.
- El jugador toca la pantalla o el botón antes de que aparezca el estímulo (salida en falso).

En modo Entrenamiento no hay game over: si cometés un error, la ronda simplemente se reinicia sin penalización.

**La barra de tiempo** en la parte superior de la pantalla va achicándose a medida que pasa el tiempo. Cambia de color para avisar:
- Verde: tiempo de sobra
- Naranja: queda menos del 30%
- Rojo: queda menos del 15%

---

### ResultActivity — Resultados

Aparece al terminar la partida (ya sea por game over o por completar los 3 niveles).

Muestra:
- Una **píldora de estado** que indica qué pasó: `✕ GAME OVER`, `★ GANASTE`, etc.
- Los **puntos totales** de la partida.
- El **tiempo promedio** de reacción en milisegundos.
- El **nivel alcanzado**.
- La **cantidad de respuestas correctas** con los tiempos mínimo y máximo.
- Un **banner de nuevo récord** si se superó el mejor puntaje anterior (en partidas no entrenamiento).

En modo Entrenamiento los puntos no se muestran ni se guardan. Sí se guardan en los modos puntuados (Fácil, Medio, Difícil).

Desde acá se puede ir a ver las estadísticas generales o volver a configurar una nueva partida.

---

### StatsActivity — Estadísticas

Muestra el historial de partidas guardadas en la base de datos, ordenadas por puntaje de mayor a menor.

Tiene dos filtros:
- **Todos:** muestra el mejor resultado de cada jugador distinto que haya jugado.
- **Solo yo:** muestra todas las partidas del jugador actual, no solo la mejor.

También tiene un botón para borrar todas las estadísticas (con confirmación previa).

Si no hay ninguna partida guardada todavía, muestra un mensaje vacío.

---

## La base de datos

La app usa **Room**, que es la librería de persistencia local de Android. Room funciona como una capa sobre SQLite: vos definís clases Java y anotaciones, y Room se encarga de generar las consultas en SQL automáticamente.

**La tabla `resultados` tiene estos campos:**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | int (auto) | Clave primaria autoincremental |
| `jugador` | String | Nombre del jugador |
| `puntos` | int | Puntaje total de la partida |
| `tiempoPromedio` | long | Promedio de tiempos de reacción en ms |
| `modo` | String | Modo de juego (Facil, Medio, Dificil) |
| `nivelAlcanzado` | int | Hasta qué nivel llegó el jugador |
| `fecha` | long | Timestamp de cuándo se jugó (epoch ms) |

**Las consultas disponibles (`ResultadoDAO`):**

```java
// Insertar una partida nueva
void insertar(ResultadoPartida resultado);

// Mejor resultado de cada jugador (para el ranking global)
List<ResultadoPartida> obtenerMejoresPorJugador();

// Todos los resultados de un jugador específico
List<ResultadoPartida> obtenerPorJugador(String nombre);

// El mejor puntaje de un jugador en un modo (para detectar récords)
Integer getMejorPuntaje(String jugador, String modo);

// Borrar todo
void eliminarTodos();
```

Como las operaciones de base de datos no se pueden hacer en el hilo principal de Android (bloquearían la UI), se ejecutan en un hilo aparte usando `Executors.newSingleThreadExecutor()`. Cuando terminan, los resultados se devuelven al hilo principal con `runOnUiThread()`.

---

## Dependencias usadas

Declaradas en `app/build.gradle`:

| Librería | Para qué se usa |
|---|---|
| `androidx.appcompat` | Base de las Activities, compatibilidad con versiones viejas de Android |
| `com.google.android.material` | Componentes visuales: botones, sliders, cards, progress indicators |
| `androidx.constraintlayout` | Sistema de layouts para posicionar vistas con relaciones entre ellas |
| `androidx.recyclerview` | Lista eficiente para mostrar las estadísticas |
| `androidx.room` | Base de datos local con SQLite |
| `androidx.activity` | Manejo del botón atrás con `OnBackPressedCallback` |

---

## Paleta de colores

El diseño sigue un tema propio llamado *Acid Lava* definido en `res/values/colors.xml`:

| Nombre | Hex | Uso |
|---|---|---|
| `bg_dark` | `#0F0D0A` | Fondo general de la app |
| `surface` | `#1A1712` | Fondo de tarjetas y elementos secundarios |
| `primary` | `#D4FF00` | Amarillo lima — color principal, botones, puntos |
| `secondary` | `#A855F7` | Violeta — nivel, modo entrenamiento |
| `correct` | `#4ADE80` | Verde — reacción correcta, barra de tiempo normal |
| `error_color` | `#FB923C` | Naranja — game over, tiempo bajo |
| `text_primary` | `#F5EED8` | Crema — texto principal |
| `text_secondary` | `#99F5EED8` | Crema al 60% — texto secundario |

---

## Tipografías

Guardadas en `res/font/`, descargadas de Google Fonts:

- **Unbounded** (`unbounded.ttf`): fuente variable, usada en las píldoras de estado y el botón de reaccionar.
- **Unbounded Black** (`unbounded_black.ttf`): peso 900, usada en títulos grandes y valores numéricos destacados.
- **DM Mono** (`dm_mono.ttf`): monoespaciada, usada en etiquetas, puntos, rondas y datos técnicos.
- **Playfair Display** (`playfair_display.ttf`): serif variable, usada en elementos decorativos como el label "puntos".

---

## Flujo completo de una partida

```
ConfigActivity
    │
    │  Intent con: nombre, modo, tiempo_max, iteraciones
    ▼
GameActivity
    │  3 niveles × N rondas
    │  CountDownTimer cada 50ms → actualiza barra de tiempo
    │  Al reaccionar → calcula puntos → siguiente ronda
    │  Al completar nivel → reduce tiempo máximo → siguiente nivel
    │  Al fallar o agotar tiempo → Game Over
    │
    │  Intent con: puntos, tiempos, nivel_alcanzado, completado
    ▼
ResultActivity
    │  Guarda en Room (hilo separado)
    │  Compara con récord anterior
    │
    │  → "JUGAR DE NUEVO" vuelve a ConfigActivity
    │  → "Ver estadísticas" va a StatsActivity
    ▼
StatsActivity
    │  Lee de Room (hilo separado)
    │  Muestra ranking con RecyclerView
    └─────────────────────────────────────────


