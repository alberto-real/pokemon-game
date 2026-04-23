# pokemon-game — backend

Spring Boot 3.5 + Java 25 backend for the daily Pokemon guessing game.

## Stack

- Spring Boot 3.5.5
- Java 25 (GraalVM CE, pinned in `.sdkmanrc`)
- Postgres 17 (via Docker Compose at repo root)
- Flyway for DB migrations
- Testcontainers for integration tests

## Prerequisites

1. SDKMAN with `java 25.0.2-graalce` installed. From repo root:
   ```bash
   sdk install java 25.0.2-graalce
   ```
2. Docker running (for Postgres + Testcontainers).

## Run the backend in local

From the repo root:

```bash
# 1. Start Postgres
docker compose -f infra/docker-compose.yml up -d

# 2. Activate Java 25 for this project (inside apps/backend/)
cd apps/backend
sdk env                       # picks 25.0.2-graalce from .sdkmanrc
# or manually:
# export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
# export PATH=$JAVA_HOME/bin:$PATH

# 3. Run Spring with the `local` profile
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Startup will:
- Run Flyway migrations (V1 initial schema)
- Ensure today's quiz exists (stub generator + stub TTS, Plan 1)
- Expose HTTP on `http://localhost:8080`

## HTTP endpoints

### Game (user flow)
- `GET  /api/game/today` — current state (bootstraps the day on first call)
- `POST /api/game/today/attempt` — body `{ "transcript": "pikachu" }`
- `POST /api/game/today/surrender`

### Quiz (user flow)
- `GET  /api/game/today/quiz` — returns 5 questions + audio paths
- `POST /api/game/today/quiz/answer/{questionId}` — body `{ "transcript": "A" }`

### Admin (stub auth only — local use)
- `POST /api/admin/quiz/generate?date=YYYY-MM-DD` (date optional, defaults to today)
- `POST /api/admin/quiz/regenerate?date=YYYY-MM-DD`
- `GET  /api/admin/quiz/status?date=YYYY-MM-DD`

### Audio
- `GET  /audio/{date}/{filename}.mp3` — serves generated MP3s

### Actuator
- `GET  /actuator/health`
- `GET  /actuator/info`

## Tests

```bash
./mvnw test
```

First run downloads the `postgres:17-alpine` image for Testcontainers.

## State of Plan 1 (current)

Plan 1 focused on the **backend foundation with stubs**. The following are in place and exercised by tests:

- Schema, entities, repos (DailyPokemon, DailyQuiz, DailyQuizQuestion, UserDailyAttempt)
- Fuzzy matchers (name + multiple-choice)
- PokeAPI client with in-memory cache
- Quiz orchestrator state machine (PENDING → GENERATING_QUIZ → GENERATING_AUDIO → READY)
- Full game + quiz REST surface (game, quiz, admin, audio)
- Startup runner + optional cron scheduler

**Stubs** — replaced in Plan 2:
- `StubQuizGenerator` (5 hardcoded questions about the Pokemon) → will be swapped for Ollama + Groq via Spring AI.
- `StubTtsService` (returns placeholder bytes) → will be swapped for Google Cloud TTS neural (es-ES).

## Configuration

Most settings are sensible defaults. Override via env vars or `-D` flags.

| Property | Default | Purpose |
|---|---|---|
| `pokemon-game.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokeAPI endpoint |
| `pokemon-game.storage.audio-dir` | `./storage/audio` | Where MP3s are saved |
| `pokemon-game.auth.stub-user` | `dev` | Stub user id until Keycloak arrives |
| `pokemon-game.scheduler.enabled` | `false` | Enable the `@Scheduled` daily job |
| `pokemon-game.scheduler.cron` | `0 0 6 * * *` | Cron expression |
| `pokemon-game.scheduler.zone` | `Europe/Madrid` | Time zone for the scheduler |

## Auth

Plan 1 uses a hardcoded `user_id = "dev"` via `CurrentUser`. Plan 3 (or post-OCI migration) will replace this with Keycloak OIDC + PKCE.

## Known limitations (addressed in later plans)

- `MultipleChoiceMatcher` prioritizes ordinal words over content substrings; an option whose text happens to be an ordinal word ("Una") is resolved as the ordinal. Use letter transcripts (A/B/C/D) for unambiguous voice answers, or reorder the rules (Plan 2 candidate).
- No real LLM or TTS wiring — Plan 2.
- No frontend — Plan 3.
