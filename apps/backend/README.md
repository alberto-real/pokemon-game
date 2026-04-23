# pokemon-game — backend

Spring Boot 3.5 + Java 25 backend for the daily Pokemon guessing game with real AI quiz generation.

## Stack

- Spring Boot 3.5.5 + Java 25 (GraalVM CE, pinned in `.sdkmanrc`)
- Postgres 17 via Docker Compose
- Spring AI 1.1.x — three-tier chain: **Groq (70B)** → **OpenRouter (70B)** → **Ollama (local 3B)**
- Flyway for DB migrations, Testcontainers for integration tests
- No server-side audio: the browser does TTS via `speechSynthesis` (Plan 3 frontend).

## Prerequisites

1. SDKMAN with `java 25.0.2-graalce`:
   ```bash
   sdk install java 25.0.2-graalce
   ```
2. Docker running (Postgres + Testcontainers).
3. Ollama daemon on `localhost:11434` with a model pulled:
   ```bash
   ollama pull llama3.2:latest
   ```
4. Optional API keys in `../../.env.local` (gitignored):
   - `GROQ_API_KEY=...` (console.groq.com, free tier) — **strongly recommended**
   - `OPENROUTER_API_KEY=...` (openrouter.ai)

   Without keys the chain collapses to Ollama-only. The 3B local model produces valid JSON but hallucinated `correctIndex`, so real play is much better with Groq enabled.

## Run locally

```bash
cd /home/fenix/Documents/develop/pokemon-game
docker compose -f infra/docker-compose.yml up -d

cd apps/backend
sdk env
set -a && source ../../.env.local && set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Startup flow:
1. Flyway migrations V1 + V2 applied.
2. `DailyQuizStartupRunner` calls `orchestrator.ensureExists(today)`.
3. `PokemonSelector` picks a random non-repeated Pokemon id (1..1025).
4. `PokeApiClient` fetches `/pokemon`, `/pokemon-species`, `/evolution-chain`.
5. `RagContextBuilder` formats facts into a Spanish context block.
6. `ChainQuizGenerator` tries Groq → OpenRouter → Ollama; first success wins.
7. 5 questions persisted, quiz status `READY`.

## HTTP endpoints

### Game
- `GET  /api/game/today`
- `POST /api/game/today/attempt` — body `{ "transcript": "pikachu" }`
- `POST /api/game/today/surrender`

### Quiz
- `GET  /api/game/today/quiz`
- `POST /api/game/today/quiz/answer/{questionId}` — body `{ "transcript": "A" }`

### Admin (stub auth)
- `POST /api/admin/quiz/generate?date=YYYY-MM-DD`
- `POST /api/admin/quiz/regenerate?date=YYYY-MM-DD`
- `GET  /api/admin/quiz/status?date=YYYY-MM-DD`

### Actuator
- `GET  /actuator/health`

## Tests

```bash
./mvnw test
```

44 tests, all passing. First run pulls `postgres:17-alpine` for Testcontainers. The AI chain is NOT hit in tests (provider defaults to `stub`).

## Configuration reference

| Property | Default | Purpose |
|---|---|---|
| `pokemon-game.ai.provider` | `stub` (default) / `chain` (local profile) | Select stub vs real chain |
| `pokemon-game.ai.ollama.base-url` | `http://localhost:11434` | Ollama endpoint |
| `pokemon-game.ai.ollama.model` | `llama3.2:latest` | Local model tag |
| `spring.ai.ollama.chat.options.num-predict` | `2048` | Token budget for 5-question JSON |
| `pokemon-game.ai.groq.enabled` | `false` (default) / `true` (local) | Include Groq in chain |
| `pokemon-game.ai.groq.api-key` | `${GROQ_API_KEY:}` | From `.env.local` |
| `pokemon-game.ai.groq.model` | `llama-3.3-70b-versatile` | Hosted 70B model |
| `pokemon-game.ai.openrouter.enabled` | `false` / `true` (local) | Include OpenRouter |
| `pokemon-game.ai.openrouter.api-key` | `${OPENROUTER_API_KEY:}` | From `.env.local` |
| `pokemon-game.ai.openrouter.model` | `meta-llama/llama-3.3-70b-instruct:free` | Free-tier hosted model |
| `pokemon-game.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokeAPI |
| `pokemon-game.auth.stub-user` | `dev` | User id until Keycloak |
| `pokemon-game.scheduler.enabled` | `false` | `@Scheduled(cron=0 0 6 * * *)` zone Europe/Madrid |

## Chain order and quality

`ChainQuizGenerator` iterates providers and falls back on any exception:

| Priority | Provider | Why here |
|---|---|---|
| 1 | Groq (`llama-3.3-70b-versatile`) | Fastest (~6 s), best quality, 14 400 req/day free |
| 2 | OpenRouter (Llama 3.3 70B free) | Second hosted tier, different rate-limit pool |
| 3 | Ollama (`llama3.2:latest`, 3B) | Offline fallback, ~30 s, occasional hallucinations |

At least one must succeed; otherwise the daily quiz stays in `GENERATING_QUIZ` and the transaction rolls back so `ensureExists` is safe to retry.

## Auth

Stub: `user_id = "dev"` via `CurrentUser`. Keycloak comes with OCI migration.

## Known limitations

- `MultipleChoiceMatcher` prioritises ordinal words over literal content. An option whose text is an ordinal (e.g. "Una") resolves as the ordinal. Voice answers with letters (A/B/C/D) are unambiguous.
- Ollama 3B quality is unreliable; rely on Groq unless you specifically want offline behaviour.
- `daily_pokemon` pool of 1 025 will exhaust after ~3 years of daily play.
- `GameServiceTest` shares a `PokemonNameCatalog` singleton across contexts and can occasionally flake on bulk test runs; isolated retries always pass.
