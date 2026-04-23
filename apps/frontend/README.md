# pokemon-game — frontend

Angular 21 standalone zoneless app with Tailwind 4 + DaisyUI 5 + ngx-translate 17.

## Prerequisites

- Node 24, npm 11, Angular CLI 21.2.x
- Backend running on `localhost:8080` (see `../backend/README.md`)

## Run

```bash
npm install
npm start
```

Open <http://localhost:4200/>. The dev server proxies `/api/*` to the backend.

## Build

```bash
npm run build
```

Artifacts in `dist/frontend/browser/`.

## Stack notes

- **Standalone components only**, no NgModules.
- **Zoneless change detection** (`provideZonelessChangeDetection`).
- **Signals** (`signal`, `computed`, `input`, `output`) for state.
- **Tailwind 4 CSS-first**: no `tailwind.config.js`. Tokens and plugins live in `src/styles.css`.
- **DaisyUI 5 plugin**: themes `light`/`dark` auto-switch based on `prefers-color-scheme`.
- **ngx-translate 17**: JSON files in `public/i18n/{es,en}.json`. Default `es`, browser detection with `localStorage` override.
- **Voice**:
  - STT: Web Speech API via `SpeechRecognizerService` (`es-ES`).
  - TTS: `window.speechSynthesis` via `TtsPlayerService` (`es-ES`).
  - Fallback to text input if `SpeechRecognition` is not supported.

## Project structure

```
src/app/
├── api/                 # HTTP types + GameApi service
├── game/                # GamePage, PokemonCanvas, QuizRunner
├── voice/               # SpeechRecognizer, TtsPlayer, VoiceRecorder component
├── i18n/                # LanguageService, LanguageSwitcher
├── landing/             # Landing page
├── admin/               # Admin page
├── app.ts               # App shell with navbar
├── app.html             # Shell template
├── app.config.ts        # Providers (router, http, translate, zoneless)
└── app.routes.ts        # Lazy routes
```

## Routes

- `/` — landing
- `/game` — today's game (lazy)
- `/admin` — admin endpoints UI (lazy)
