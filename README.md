# pokemon-game

Juego diario de Pokemon: imagen difuminada que se revela intento a intento, reconocimiento de voz para adivinar el nombre y quiz de 5 preguntas generado por IA sobre ese Pokemon. TTS en el navegador.

## Estructura

- `apps/frontend/` — Angular 21 standalone zoneless + Tailwind 4 + DaisyUI 5 + ngx-translate (es/en).
- `apps/backend/` — Spring Boot 3.5 + Java 25 (GraalVM CE) + Spring AI con cadena Groq → OpenRouter → Ollama.
- `infra/docker-compose.yml` — Postgres 17 (Ollama corre como servicio del host).
- `docs/superpowers/` — specs y planes de implementación.

## Arrancar en local

```bash
# 1. Postgres
docker compose -f infra/docker-compose.yml up -d

# 2. Backend (terminal 1)
cd apps/backend
sdk env
set -a && source ../../.env.local && set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# 3. Frontend (terminal 2)
cd apps/frontend
npm install   # solo la primera vez
npm start
```

Abre <http://localhost:4200/>. Requisitos:

- SDKMAN con `java 25.0.2-graalce`.
- Node 24 + npm 11 + Angular CLI 21.2.x.
- Ollama en `localhost:11434` con `ollama pull llama3.2:latest`.
- Opcional en `.env.local` (gitignored): `GROQ_API_KEY` y `OPENROUTER_API_KEY` para mejor calidad de quiz.

## Planes ejecutados

- [Plan 1 — Backend foundation](docs/superpowers/plans/2026-04-22-plan-1-backend-foundation.md) (stubs de IA y TTS)
- [Plan 2 — Real AI integration](docs/superpowers/plans/2026-04-23-plan-2-real-ai-integration.md) (Groq + OpenRouter + Ollama, sin TTS server)
- [Plan 3 — Frontend Angular](docs/superpowers/plans/2026-04-23-plan-3-frontend-angular.md) (UI completa con voz y TTS del navegador)

## Roadmap (fuera del MVP)

- Sub-proyecto `notification-service` (cola + Web Push + preferencias por usuario).
- Migración a OCI: monorepo Nx, Keycloak, GraalVM Native Image, Cloudflare Tunnel.
