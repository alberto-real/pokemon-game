# pokemon-game — Diseño

- **Fecha**: 2026-04-22
- **Autor**: Alberto Real (con brainstorming asistido por Claude)
- **Estado**: Propuesta — pendiente de revisión y aprobación

## 1. Resumen ejecutivo

`pokemon-game` es una app web de juego diario estilo Wordle sobre Pokemon: cada día se revela progresivamente una imagen difuminada de un Pokemon que el usuario debe adivinar por voz. Tras acertar o rendirse se abre un quiz de 5 preguntas de opción múltiple sobre ese Pokemon, respondible también por voz y con las preguntas leídas en alto mediante el `speechSynthesis` nativo del navegador.

Es el primero de dos sub-proyectos. Un segundo proyecto, `notification-service` (fuera de alcance de este spec), añadirá notificaciones push diarias transversales a cualquier app.

Target inicial: **local-first**, self-contained, portable hacia una futura plataforma personal en OCI (Nx monorepo + microfronts + monolito modular Spring + Keycloak + Cloudflare Tunnel) sin reescritura.

## 2. Objetivos y no-objetivos

### Objetivos
- Mecánica jugable end-to-end en local (frontend + backend + DB + LLM + TTS).
- Un único Pokemon diario compartido (Wordle-style).
- Reconocimiento de voz para adivinar el nombre y responder el quiz, sin IA en el pipeline de voz.
- Generación del quiz con LLM local (Ollama) y fallback a Groq free tier.
- Audio TTS de las preguntas reproducido por el navegador vía `speechSynthesis` (`es-ES`), sin backend de audio.
- Auto-detección del tema (claro/oscuro) del sistema operativo del usuario.
- **UI bilingüe** (castellano por defecto, inglés disponible) con `@ngx-translate/core`. El contenido del juego (voz, preguntas, Pokémon) permanece en castellano.
- Código portable al futuro monorepo Nx + Keycloak + Cloudflare Tunnel en OCI, sin reescritura.

### No-objetivos (explícitamente fuera de alcance)
- Push notifications (→ `notification-service`, sub-proyecto separado).
- Keycloak / OIDC / multi-usuario real (→ al migrar a OCI; aquí auth stub).
- Microfronts / Native Federation (→ al migrar; aquí app Angular standalone).
- GraalVM Native Image (→ al migrar; el código será compatible, pero se compila en JVM en local).
- Observabilidad completa (OTel/Grafana/Loki) (→ al migrar).
- Cola de mensajería (→ se introduce con `notification-service`).
- Backfill de quizzes pasados.
- Ranking, rachas, estadísticas sociales (fuera del MVP).
- Generación de audio server-side (Google Cloud TTS, Piper, Coqui). El navegador hace TTS.
- Almacenamiento de MP3s en disco. No hay audio persistido.

## 3. Alcance y sub-proyectos

Este spec cubre **solo** `pokemon-game`. La ruta global del proyecto personal de Alberto es:

1. `pokemon-game` sin push ← **este spec**
2. `notification-service` (transversal, reusable: cola + Web Push + preferencias por usuario)
3. Integración: `pokemon-game` publica eventos, `notification-service` los consume
4. Migración a plataforma OCI (Nx monorepo, Keycloak, Native)

## 4. Arquitectura de alto nivel

```
┌────────────────────── pokemon-game (local) ─────────────────────┐
│                                                                 │
│  [ Angular 21 app ] ── HTTP ──▶ [ Spring Boot API ]             │
│       │                              │                          │
│       │ Web Speech API (STT)         │ JPA / Flyway             │
│       │ speechSynthesis (TTS)        ▼                          │
│       │                         [ Postgres 17 ]                 │
│       │                              │                          │
│       │                              │ Spring AI                │
│       │                              ▼                          │
│       │                  [ Ollama (Docker) ]                    │
│       │                  llama3.2:3b                            │
│       │                              │ fallback (env)           │
│       │                              ▼                          │
│       │                  [ Groq + OpenRouter ]                  │
│                                                                 │
│  Datos externos: PokeAPI                                        │
└─────────────────────────────────────────────────────────────────┘
```

## 5. Mecánica del juego

Flujo de una partida diaria:

1. Usuario abre la app.
2. Frontend pide `GET /api/game/today`. Backend invoca `ensureTodaysQuizExists()` (idempotente). Si no está listo, responde `202` con estado; si `READY`, devuelve el estado inicial del día.
3. UI muestra:
   - Imagen del Pokemon en modo silueta (shadow negra, opacidad 100%).
   - Botón grande de grabar (voz).
   - Contador: "5 intentos".
4. Usuario pulsa grabar, habla → Web Speech API transcribe en `es-ES` → frontend envía el transcript crudo al backend.
5. Backend normaliza (tildes, mayúsculas) y hace fuzzy matching (Levenshtein ratio ≥ 0.75) contra lista cerrada de nombres Pokemon.
6. Si falla: backend responde `correct=false` + nuevo `blurLevel` + intentos restantes. Progresión visual:
   - Intento 1 → silueta negra (filter: brightness(0))
   - Intento 2 → blur 40px
   - Intento 3 → blur 20px
   - Intento 4 → blur 10px
   - Intento 5 → imagen nítida (blur 0)
7. Si acierta: revela el Pokemon + nombre, score nombre = `5 - (intentos_usados - 1)` (max 5, min 1).
8. Si agota los 5 intentos o pulsa "Me rindo" antes: revela igual, score nombre = 0.
9. Transición al **Quiz** (siempre obligatorio, acierte o se rinda).
10. Quiz: 5 preguntas de opción múltiple. Cada pregunta se lee con `speechSynthesis` del navegador al mostrarla. Usuario responde por voz → frontend envía transcript → backend lo parsea (letra / ordinal / contenido) y valida. 1 pt por acierto.
11. Score final del día = `nombre (0-5) + quiz (0-5)` = `0-10`.
12. Se persiste en `user_daily_attempt`.

## 6. Modelo de datos

```sql
-- Pokemon del día (uno por fecha, compartido entre todos los usuarios)
CREATE TABLE daily_pokemon (
  date DATE PRIMARY KEY,
  pokemon_id INT NOT NULL,            -- ID de PokeAPI (1..1025)
  pokemon_name TEXT NOT NULL,         -- nombre canónico en inglés (para matching)
  pokemon_name_es TEXT NOT NULL,      -- nombre en castellano (para display)
  image_url TEXT NOT NULL,            -- official-artwork de PokeAPI
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Quiz del día con estado de generación
CREATE TABLE daily_quiz (
  date DATE PRIMARY KEY REFERENCES daily_pokemon(date) ON DELETE CASCADE,
  status TEXT NOT NULL,               -- PENDING | GENERATING_QUIZ | READY | FAILED
  status_message TEXT,                -- diagnóstico si FAILED
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ready_at TIMESTAMPTZ
);

-- Preguntas del quiz (5 por daily_quiz)
CREATE TABLE daily_quiz_question (
  id BIGSERIAL PRIMARY KEY,
  quiz_date DATE NOT NULL REFERENCES daily_quiz(date) ON DELETE CASCADE,
  position INT NOT NULL,
  question_text TEXT NOT NULL,
  options JSONB NOT NULL,
  correct_option_index INT NOT NULL,
  CONSTRAINT uq_quiz_position UNIQUE (quiz_date, position)
);

-- Estado por usuario
CREATE TABLE user_daily_attempt (
  user_id TEXT NOT NULL,              -- "dev" en stub; Keycloak sub en futuro
  date DATE NOT NULL,
  name_attempts_used INT NOT NULL DEFAULT 0,
  name_solved BOOLEAN NOT NULL DEFAULT FALSE,
  name_surrendered BOOLEAN NOT NULL DEFAULT FALSE,
  name_score INT,
  quiz_answers JSONB,                 -- [{position, selected_index, correct}]
  quiz_score INT,
  total_score INT,
  completed_at TIMESTAMPTZ,
  PRIMARY KEY (user_id, date)
);
```

Notas:
- `UNIQUE` en `daily_pokemon.date` y `daily_quiz.date` garantiza idempotencia de la generación ante concurrencia.
- `daily_quiz.status` permite polling limpio desde el cliente.
- `user_id = 'dev'` fijo en auth stub; se reemplazará por el `sub` de Keycloak sin cambios de esquema.

## 7. API HTTP (backend)

### Públicos (frontend)
- `GET  /api/game/today` — estado del día: blur level actual, intentos restantes, `quiz_status`.
- `POST /api/game/today/attempt` — body `{ transcript }`. Response `{ correct, blurLevel, attemptsLeft, revealed? }`.
- `POST /api/game/today/surrender` — Response `{ pokemon, quizReady }`.
- `GET  /api/game/today/quiz` — preguntas (si `READY`).
- `POST /api/game/today/quiz/answer` — body `{ questionId, transcript }`. Response `{ correct, selectedIndex, nextQuestion? | finalScore }`.

### Admin (stub auth, solo en local)
- `POST /api/admin/quiz/generate?date=YYYY-MM-DD`
- `POST /api/admin/quiz/regenerate?date=YYYY-MM-DD`
- `GET  /api/admin/quiz/status?date=YYYY-MM-DD`

## 8. Generación diaria del quiz

```
ensureTodaysQuizExists()    (idempotente, seguro ante concurrencia por UNIQUE(date))
  │
  ├─ 1. Seleccionar Pokemon
  │     · Random INT 1..1025 excluyendo IDs ya usados en daily_pokemon
  │     · Fetch PokeAPI: /pokemon/{id}, /pokemon-species/{id}, /evolution-chain/{id}
  │     · INSERT INTO daily_pokemon ... ON CONFLICT DO NOTHING
  │
  ├─ 2. INSERT INTO daily_quiz (status=PENDING)
  │
  ├─ 3. [async] status=GENERATING_QUIZ
  │     · Prompt RAG con datos de PokeAPI como contexto
  │     · Spring AI → Ollama (provider por defecto) con JSON schema forzado
  │     · Fallback a Groq si Ollama falla o si config lo indica
  │     · INSERT INTO daily_quiz_question × 5
  │
  └─ 4. status=READY, ready_at=NOW()
```

**Triggers de invocación** (cualquiera de las tres, todas convergen al mismo método idempotente):
- `ApplicationReadyEvent` listener al arrancar Spring Boot.
- `@Scheduled(cron = "${pokemon-game.scheduler.cron}")` sólo si `pokemon-game.scheduler.enabled=true` (por defecto `false` en local, `true` en perfil prod futuro).
- Endpoint admin manual (`POST /api/admin/quiz/generate`).

**Errores**:
- Ollama falla → reintenta 2 veces con backoff; si falla y `pokemon-game.ai.groq.enabled=true`, usa Groq; si también falla → `status=FAILED`.

## 9. Frontend (Angular 21)

### Stack
- **Angular 21** standalone (sin NgModules), zoneless.
- **Signals** + `linkedSignal()` + `computed()` para estado del juego.
- **`httpResource()`** (signal-based HTTP en v21) para fetch del estado + polling adaptativo.
- **Signal Forms v21** (si hay formularios; casi todo son botones y voz).
- **Tailwind CSS 4.2.x** (CSS-first config, `@import "tailwindcss"`, sin `tailwind.config.js` salvo extender tokens).
- **DaisyUI 5.5.x** como plugin CSS:
  ```css
  @plugin "daisyui" {
    themes: light --default, dark --prefersdark;
  }
  ```
  El tema se respeta **automáticamente** del SO del usuario vía `prefers-color-scheme` — cero JS extra.
- **Vitest** + TestBed para tests.

### Componentes clave
- `GamePage` — orquesta el flujo del día, estado global con signals.
- `PokemonCanvas` — muestra la imagen con filtros dinámicos (`brightness(0)` / `blur(Xpx)`) según `blurLevel` signal.
- `VoiceRecorder` — wrapper sobre Web Speech API, emite transcript.
- `QuizRunner` — itera preguntas, reproduce la pregunta con `speechSynthesis`, captura respuesta.
- `ScoreBoard` — muestra resultado final con componentes DaisyUI (card, badge, progress).
- `AdminPage` — endpoints admin (stub auth).

### Servicios (inyectados vía `inject()`)
- `GameApi` — wrapper sobre `httpResource()` y `HttpClient` para endpoints del juego.
- `SpeechRecognizer` (interfaz) + `WebSpeechRecognizer` (implementación) + `NoopRecognizer` (fallback si no hay soporte).
- `TtsPlayer` — envuelve `speechSynthesis` (voz `es-ES`), reproducción secuencial/on-demand.
- `ApiClient` — tipos compartidos (generados desde OpenAPI o manuales).

### Rutas
- `/` — landing con botón "Empezar juego del día".
- `/game` — lazy `GamePage`.
- `/admin` — lazy `AdminPage`.

### Componentes DaisyUI utilizados
- `btn` — botones principales (grabar, rendirse, continuar).
- `card` — contenedor de Pokemon, tarjetas de pregunta.
- `modal` — revelación del Pokemon, resultado final.
- `progress` — barra de intentos usados.
- `alert` — feedback correcto/incorrecto.
- `kbd` — mostrar "A / B / C / D" sobre las opciones.
- `toast` — notificaciones efímeras ("Voz no detectada, intenta de nuevo").

### Internacionalización (i18n) — dos capas, una sola bilingüe

**Capa 1 (UI chrome): bilingüe con `@ngx-translate/core` 17.x**
- Idiomas soportados: `es` (por defecto) y `en`.
- Detección al arranque: `navigator.language` → si empieza por `es` → `es`, si no → `en`, fallback final `es`.
- Selector manual en un componente de header (`LanguageSwitcher`) que persiste la elección en `localStorage`.
- Ficheros de traducción: `src/assets/i18n/es.json` (source of truth) + `src/assets/i18n/en.json`.
- Claves organizadas por feature: `game.attempts.remaining`, `quiz.question.label`, `common.surrender`, `errors.voice_not_detected`, etc.
- `HttpLoaderFactory` con `TranslateHttpLoader` para cargar los JSON desde `/assets/i18n/`.

**Capa 2 (contenido del juego): monolingüe castellano**
- STT: `lang='es-ES'` fijo.
- TTS: `speechSynthesis` del navegador con voz `es-ES`. No se genera audio en inglés.
- LLM: genera el quiz en castellano. No se generan quizzes paralelos en inglés.
- Nombres Pokemon: el `pokemon_name_es` se muestra al usuario; el `pokemon_name` (inglés canónico) se usa internamente para matching.
- Consecuencia: aunque el usuario cambie la UI a inglés, las preguntas, respuestas y audio del quiz siguen siendo en castellano. La UX es coherente: "juego en castellano con UI en el idioma preferido del usuario".

**Scope futuro (fuera de este spec)**: bilingüismo completo (STT/TTS/LLM en ambos idiomas) → Plan separado si se necesita.

## 10. Backend (Spring Boot 3.5 + Java 25)

### Stack
- Spring Boot 3.5.x, Java 25.
- Código compatible con GraalVM Native (sin reflection escondida; `@RegisterReflection` donde haga falta). Compilación JVM en local.
- **Spring AI** para abstracción LLM (Ollama primario, Groq fallback).
- Spring Data JPA + Postgres JDBC.
- Flyway para migraciones desde día 1.
- WebFlux opcional para clientes HTTP no bloqueantes (PokeAPI).

### Módulos (packages dentro del monolito modular)
- `game` — core del juego, daily state, scoring.
- `quiz` — generación del quiz, preguntas, respuestas.
- `pokeapi` — cliente HTTP a PokeAPI con cache en memoria.
- `ai` — abstracción `QuizGenerator`, adapters Ollama/Groq.
- `admin` — endpoints de administración (stub auth).
- `common` — DTOs, errores, utilidades, normalización.

### Configuración
```properties
# application.properties (defaults locales)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:3b
spring.ai.ollama.chat.options.format=json

pokemon-game.scheduler.enabled=false
pokemon-game.scheduler.cron=0 0 6 * * *
pokemon-game.scheduler.zone=Europe/Madrid

pokemon-game.ai.provider=stub
pokemon-game.ai.groq.enabled=false
pokemon-game.ai.groq.api-key=${GROQ_API_KEY:}
pokemon-game.ai.openrouter.enabled=false
pokemon-game.ai.openrouter.api-key=${OPENROUTER_API_KEY:}

pokemon-game.auth.stub-user=dev
```

## 11. Reconocimiento de voz y TTS

### STT (voz → texto) — cero IA
- Cliente: Web Speech API (`SpeechRecognition`) con `lang='es-ES'`.
- Fallback: input de texto si el navegador no soporta la API.
- Post-procesado en el servidor: normalización Unicode + fuzzy matching (Levenshtein ratio ≥ 0.75) contra lista cerrada.

### TTS (texto → voz) — cliente nativo, cero coste, cero infraestructura
- Cliente: `speechSynthesis.speak(new SpeechSynthesisUtterance(text))` con `voice.lang='es-ES'`.
- No hay backend de audio ni MP3s almacenados.
- Calidad: la del TTS nativo del SO (Siri en iOS, Google TTS en Android, voces del sistema en desktop).
- Trade-off aceptado: voz menos premium que neural cloud, a cambio de simplicidad total.
- Si en el futuro se quiere audio neural: se añade `TextToSpeechService` como interfaz + adaptador (ElevenLabs, Google Cloud TTS…). No bloquea nada del diseño actual.

## 12. Estrategia de testing

### Backend
- Unit: scoring, matching, selección aleatoria no repetitiva, normalización.
- Integration (**Testcontainers**): Postgres real; mocks para PokeAPI, Ollama.
- Tests de idempotencia de `ensureTodaysQuizExists()` con invocaciones concurrentes.

### Frontend
- Vitest + TestBed.
- Unit: `PokemonNameMatcher`, `MultipleChoiceMatcher` con casos de pronunciación reales.
- Component: `PokemonCanvas` con cada `blurLevel`, `QuizRunner` flujo completo.
- Mock de Web Speech API.

Sin tests E2E automáticos en MVP (YAGNI).

## 13. Infraestructura local (`infra/docker-compose.yml`)

Servicios:
- **`postgres:17`** con volumen persistente y healthcheck.
- **`ollama/ollama`** con modelo `llama3.2:3b` (o `qwen2.5:7b`) pre-pull al primer arranque; healthcheck.
- (Opcional) `backend` y `frontend` dockerizados para arranque completo; en desarrollo típico se ejecutan desde IDE.

Volúmenes:
- Volumen named para Ollama models cache.

## 14. Secrets y configuración local

- `GROQ_API_KEY` (opcional, activa el segundo tier de la cadena IA) → `.env.local` (gitignored).
- `OPENROUTER_API_KEY` (opcional, activa el tercer tier) → `.env.local`.
- Sin ellas, Ollama local cubre la generación de quiz.

## 15. Migración futura a OCI (contexto, no alcance aquí)

1. Crear monorepo Nx 20 con pnpm en un repo nuevo (o refactor de este).
2. Mover `apps/frontend/` → `apps/pokemon-ui/` en el monorepo, configurarlo como remote de Native Federation.
3. Mover `apps/backend/` → `modules/pokemon/` en el monolito modular Spring.
4. Añadir Keycloak 26 (realm, client, PKCE desde Angular, validación JWT en gateway Spring).
5. Compilar backend con GraalVM Native.
6. Docker Compose en OCI A1, expuesto por Cloudflare Tunnel.
7. Crear `notification-service`, conectar cola (Rabbit o Redis Streams) + Web Push + preferencias por usuario.
8. Añadir observabilidad (OTel → Loki/Tempo/Prometheus/Grafana).

El diseño actual no bloquea ninguno de estos pasos: folder layout Nx-compatible, código Native-compatible, auth abstraída detrás de un `CurrentUser` inyectable.

## 16. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Web Speech API funciona mal en Android/Firefox | Fallback a input texto + futuro Whisper |
| Ollama consume demasiada RAM en el laptop | Modelo 3B Q4; alternativamente Groq free tier (14 400 req/día) |
| Calidad del quiz generado (alucinaciones) | RAG: pasar datos de PokeAPI como contexto, prohibir conocimiento paramétrico |
| PokeAPI rate limit (100/min) | Cache en memoria; 1 Pokemon/día consume <5 requests |
| Reflection Angular/Spring rompe Native en migración | Desde día 1: evitar reflexión, usar `@RegisterReflection` donde aplique |

---

## Apéndice A — Versiones fijadas

| Pieza | Versión objetivo |
|---|---|
| Node.js | 24.12.0 (actual local) |
| npm | 11.x |
| Angular CLI / framework | 21.2.x |
| Tailwind CSS | 4.2.x |
| DaisyUI | 5.5.x |
| @ngx-translate/core + /http-loader | 17.0.x |
| Java | 25 (GraalVM CE 25.0.2) |
| Spring Boot | 3.5.x |
| Spring AI | 1.0.x (compatible con Spring Boot 3.5) |
| Postgres | 17 |
| Ollama | última estable + modelo llama3.2:3b |
| Flyway | alineado con Spring Boot 3.5 BOM |
