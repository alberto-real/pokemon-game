# pokemon-game — Plan 2: Real AI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar los stubs de QuizGenerator y TextToSpeech por IA real (cadena Ollama → Groq → OpenRouter con fallback automático) y eliminar todo el pipeline de generación y almacenamiento de audio del backend (el TTS pasará a ser responsabilidad del navegador vía `speechSynthesis` en Plan 3).

**Architecture:** Spring AI 1.x con tres proveedores configurados como beans separados: `OllamaChatModel` (primario, local, sin coste), más dos `OpenAiChatModel` con base URLs personalizadas apuntando a Groq y OpenRouter (ambos usan la API OpenAI-compatible). Un `ChainQuizGenerator` los encadena con fallback en excepción. Prompt RAG con datos estructurados de PokeAPI para eliminar alucinaciones.

**Tech Stack:** Spring AI 1.0.x, Ollama (llama3.2:3b en Docker), Groq (Llama 3.3 70B free tier), OpenRouter (Llama 3.3 70B free). JSON mode para salida estructurada. Flyway V2 para eliminar columnas de audio.

**Referencias:**
- Plan 1 completado: `docs/superpowers/plans/2026-04-22-plan-1-backend-foundation.md`
- Spec: `docs/superpowers/specs/2026-04-22-pokemon-game-design.md` (actualizar en Task 1)

---

## Convenciones

- Working dir: `/home/fenix/Documents/develop/pokemon-game/`.
- Backend: `apps/backend/`, paquete raíz `com.albertoreal.pokemongame`.
- Rama activa: `feat/backend-foundation` (Plan 1 terminado). Continuamos en la misma rama.
- Git identity: **siempre** del local `git config`. NUNCA `-c user.name/email` overrides. **NUNCA push**.
- Java env obligatorio en cada `./mvnw`:
  ```bash
  export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
  export PATH=$JAVA_HOME/bin:$PATH
  ```
- Claves API están en `.env.local` (gitignored). El backend las lee vía Spring propiedades con referencia a variables de entorno.

---

## Task 1: Actualizar spec — quitar Google Cloud TTS

**Files:**
- Modify: `docs/superpowers/specs/2026-04-22-pokemon-game-design.md`

- [ ] **Step 1: Leer el spec y localizar las secciones afectadas**

```bash
grep -n "Google Cloud TTS\|TextToSpeech\|audio\|MP3\|Chirp3\|TTS" docs/superpowers/specs/2026-04-22-pokemon-game-design.md | head -30
```

- [ ] **Step 2: Editar §2 Objetivos — sustituir la línea sobre audio pre-generado**

Cambiar:
```
- Audio TTS de las preguntas, pre-generado con Google Cloud TTS neural y cacheado.
```
Por:
```
- Audio TTS de las preguntas reproducido por el navegador vía `speechSynthesis` (`es-ES`), sin backend de audio.
```

- [ ] **Step 3: Editar §2 No-objetivos — añadir**

En la lista de no-objetivos, añadir:
```
- Generación de audio server-side (Google Cloud TTS, Piper, Coqui). El navegador hace TTS.
- Almacenamiento de MP3s en disco. No hay audio persistido.
```

- [ ] **Step 4: Editar §4 Arquitectura — eliminar el bloque de Google Cloud TTS y MP3 estáticos**

Localiza el diagrama ASCII y elimina las líneas sobre TTS + MP3 estáticos. La arquitectura queda:

```
[ Angular 21 app ] ── HTTP ──▶ [ Spring Boot API ]
       │                              │
       │ Web Speech API (STT)          │ JPA / Flyway
       │ speechSynthesis (TTS)         ▼
       │                         [ Postgres 17 ]
       │                              │
       │                              │ Spring AI
       │                              ▼
       │                  [ Ollama (Docker) ]
       │                  llama3.2:3b
       │
       │                  [ Groq + OpenRouter fallback ]
       │
Datos externos: PokeAPI
```

- [ ] **Step 5: Reemplazar §11 completo**

Busca la sección `## 11. Reconocimiento de voz y TTS` y reemplázala por:

```markdown
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
```

- [ ] **Step 6: Editar §7 API HTTP — eliminar sección "Recursos estáticos"**

Borra la línea:
```
- `GET /audio/{quiz_date}/{filename}.mp3` — servidos directamente por Spring Boot desde `./storage/audio/`.
```
Y su header `### Recursos estáticos`.

- [ ] **Step 7: Editar §6 Modelo de datos — actualizar `daily_quiz_question`**

La tabla `daily_quiz_question` pierde `audio_question_path` y `audio_options_paths`. Editar el bloque SQL removiendo esas dos líneas. La tabla queda:

```sql
CREATE TABLE daily_quiz_question (
  id BIGSERIAL PRIMARY KEY,
  quiz_date DATE NOT NULL REFERENCES daily_quiz(date) ON DELETE CASCADE,
  position INT NOT NULL,
  question_text TEXT NOT NULL,
  options JSONB NOT NULL,
  correct_option_index INT NOT NULL,
  CONSTRAINT uq_quiz_position UNIQUE (quiz_date, position)
);
```

- [ ] **Step 8: Editar §8 — eliminar fase GENERATING_AUDIO**

La máquina de estados queda: `PENDING → GENERATING_QUIZ → READY | FAILED`. Eliminar las líneas sobre Google TTS y MP3s en el bloque ASCII del diagrama.

- [ ] **Step 9: Editar §10 Módulos — eliminar `media`**

En la lista de packages del backend elimina la entrada `media`.

- [ ] **Step 10: Editar §14 Secrets — quitar referencias a `gcp-tts.json`**

Eliminar la línea sobre el service account JSON. Dejar solo `GROQ_API_KEY` y añadir `OPENROUTER_API_KEY`.

- [ ] **Step 11: Editar §16 Riesgos — eliminar fila de Google Cloud TTS**

Quitar la fila "Google Cloud TTS free tier se agota" de la tabla.

- [ ] **Step 12: Editar Apéndice A Versiones**

Eliminar cualquier referencia a Google Cloud TTS. Añadir:
```
| Spring AI | 1.0.x (compatible con Spring Boot 3.5) |
```

- [ ] **Step 13: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add docs/superpowers/specs/
git commit -m "docs(spec): remove Google Cloud TTS, use browser speechSynthesis instead"
```

---

## Task 2: Añadir Ollama al `docker-compose.yml`

**Files:**
- Modify: `infra/docker-compose.yml`
- Create: `infra/ollama-init.sh`

- [ ] **Step 1: Reemplazar `infra/docker-compose.yml` por esta versión**

```yaml
services:
  postgres:
    image: postgres:17-alpine
    container_name: pokemon-game-postgres
    environment:
      POSTGRES_DB: pokemon_game
      POSTGRES_USER: pokemon
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-pokemon_dev_password}
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pokemon -d pokemon_game"]
      interval: 5s
      timeout: 3s
      retries: 10

  ollama:
    image: ollama/ollama:latest
    container_name: pokemon-game-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
      - ./ollama-init.sh:/ollama-init.sh:ro
    entrypoint: ["/bin/sh", "-c"]
    command: ["ollama serve & sleep 5 && /ollama-init.sh && wait"]
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 60s

volumes:
  postgres-data:
  ollama-data:
```

- [ ] **Step 2: Crear `infra/ollama-init.sh` que haga pull del modelo idempotente**

```bash
#!/bin/sh
# Pulls the default Ollama model if not already present.
set -e

MODEL="${OLLAMA_MODEL:-llama3.2:3b}"

echo "[ollama-init] Checking if model $MODEL is present..."
if ollama list 2>/dev/null | grep -q "^$MODEL"; then
    echo "[ollama-init] Model $MODEL already present, skipping pull."
else
    echo "[ollama-init] Pulling model $MODEL (this takes 2-3 minutes on first run)..."
    ollama pull "$MODEL"
    echo "[ollama-init] Pull done."
fi
```

Make it executable:
```bash
chmod +x /home/fenix/Documents/develop/pokemon-game/infra/ollama-init.sh
```

- [ ] **Step 3: Levantar y verificar**

```bash
cd /home/fenix/Documents/develop/pokemon-game
docker compose -f infra/docker-compose.yml up -d
```

Wait 30 seconds for Ollama to start serving, then confirm Postgres is still healthy:
```bash
docker compose -f infra/docker-compose.yml ps
```
Expected: `postgres` healthy, `ollama` running (healthy after the model pull completes, which can take 2-3 min).

Wait for Ollama model pull (check logs):
```bash
docker logs -f pokemon-game-ollama 2>&1 | grep -E "Pulling|Pull done|already present" &
LOG_PID=$!
# Poll until the model is ready, max 10 min
for i in $(seq 1 60); do
    if docker exec pokemon-game-ollama ollama list 2>/dev/null | grep -q "llama3.2:3b"; then
        echo "Model ready"
        break
    fi
    sleep 10
done
kill $LOG_PID 2>/dev/null || true
docker exec pokemon-game-ollama ollama list
```

Expected: listing with `llama3.2:3b` present.

- [ ] **Step 4: Smoke test directo**

```bash
curl -s http://localhost:11434/api/generate -d '{
  "model": "llama3.2:3b",
  "prompt": "Responde SOLO con un JSON: {\"pong\": true}",
  "stream": false,
  "format": "json"
}' | head -c 200
```
Expected: JSON with `pong` field. Prueba que Ollama responde.

- [ ] **Step 5: Commit**

```bash
git add infra/
git commit -m "chore(infra): add Ollama service with llama3.2:3b to docker-compose"
```

---

## Task 3: Añadir Spring AI al backend

**Files:**
- Modify: `apps/backend/pom.xml`

- [ ] **Step 1: Añadir Spring AI BOM en `<dependencyManagement>` del pom**

Localiza (o crea) el bloque `<dependencyManagement>` del pom y añade el BOM de Spring AI. Spring Initializr incluye `<dependencyManagement>` con el `spring-boot-starter-parent`; conviene añadir el BOM de Spring AI para fijar versiones compatibles.

Encuentra `<parent>` en el pom y verifica que es `spring-boot-starter-parent` 3.5.x. Luego añade el BOM:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

(Si ya existe `<dependencyManagement>`, añade solo el `<dependency>`.)

Si el engineer detecta que `1.0.0` no resuelve, probar `1.0.1`, `1.0.2`, etc. Consultar <https://mvnrepository.com/artifact/org.springframework.ai/spring-ai-bom> si es necesario.

- [ ] **Step 2: Añadir las dos dependencias starter dentro de `<dependencies>`**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

- [ ] **Step 3: Verificar que resuelve**

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
export PATH=$JAVA_HOME/bin:$PATH
./mvnw -B dependency:tree | grep -i "spring-ai" | head -20
```
Expected: al menos ocho dependencias spring-ai resueltas (autoconfig, core, openai, ollama…).

- [ ] **Step 4: Verificar compila**

```bash
./mvnw -B compile 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/pom.xml
git commit -m "chore(backend): add Spring AI BOM, Ollama and OpenAI starters"
```

---

## Task 4: Flyway V2 — quitar columnas de audio de `daily_quiz_question`

**Files:**
- Create: `apps/backend/src/main/resources/db/migration/V2__remove_audio_columns.sql`

- [ ] **Step 1: Crear migración**

```sql
ALTER TABLE daily_quiz_question DROP COLUMN IF EXISTS audio_question_path;
ALTER TABLE daily_quiz_question DROP COLUMN IF EXISTS audio_options_paths;
```

- [ ] **Step 2: Aplicar migración arrancando la app brevemente**

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
export PATH=$JAVA_HOME/bin:$PATH
SPRING_PROFILES_ACTIVE=local ./mvnw -B spring-boot:run > /tmp/spring-v2.log 2>&1 &
SPRING_PID=$!
```

Espera ~20 segundos y verifica:
```bash
grep -i "migrat\|flyway\|error" /tmp/spring-v2.log | head
```
Expected: `Successfully applied 1 migration` (V2) o bien `Current version of schema "public": 2`.

**Nota**: la migración rompe la compilación si las entidades aún usan los campos. Por eso esta task va antes de borrar los campos en la entidad (Task 5). Al arrancar ahora, Hibernate intentará validar la entidad contra la columna ausente y fallará el startup. Para esta task:
- **Si el startup falla con validación Hibernate**, es lo esperado: la migración ya aplicó. Kill el proceso y continúa.
- **Si no falla** (poco probable), espera los `\dt` checks siguientes.

```bash
kill $SPRING_PID 2>/dev/null || true
pkill -f spring-boot:run 2>/dev/null || true
```

Verifica en la DB que las columnas se fueron:
```bash
docker exec pokemon-game-postgres psql -U pokemon -d pokemon_game \
  -c "\d daily_quiz_question"
```
Expected: listing SIN las columnas `audio_question_path` y `audio_options_paths`.

- [ ] **Step 3: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/main/resources/db/migration/
git commit -m "feat(backend): V2 migration — remove audio columns (browser TTS)"
```

---

## Task 5: Actualizar entidad `DailyQuizQuestion` — quitar campos de audio

**Files:**
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/DailyQuizQuestion.java`

- [ ] **Step 1: Reemplazar el archivo entero por esta versión sin campos de audio**

```java
package com.albertoreal.pokemongame.quiz;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "daily_quiz_question")
public class DailyQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @Column(nullable = false)
    private int position;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    protected DailyQuizQuestion() {}

    public DailyQuizQuestion(LocalDate quizDate, int position, String questionText,
                             List<String> options, int correctOptionIndex) {
        this.quizDate = quizDate;
        this.position = position;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public Long getId() { return id; }
    public LocalDate getQuizDate() { return quizDate; }
    public int getPosition() { return position; }
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
}
```

- [ ] **Step 2: Compilar**

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
./mvnw -B compile 2>&1 | tail -10
```
**Esperado FAIL**: hay lugares que usan `setAudioQuestionPath`/`setAudioOptionsPaths` (QuizOrchestrator, QuizView). Tareas siguientes los arreglan.

Eso es OK para esta task — solo queremos el entity limpio. Skip commit y continuar.

- [ ] **Step 3: Commit parcial (entidad lista, compilación rota intencionadamente)**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/DailyQuizQuestion.java
git commit -m "feat(backend): drop audio fields from DailyQuizQuestion entity (WIP, breaks build)"
```

> **Nota**: normalmente no commiteamos builds rotos. Aceptable aquí porque es un paso intermedio de una refactor coordinada y la siguiente task (6) restaura la build.

---

## Task 6: Simplificar `QuizOrchestrator` — quitar fase de audio, quitar estado GENERATING_AUDIO

**Files:**
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/QuizOrchestrator.java`

- [ ] **Step 1: Reemplazar el orchestrator por esta versión**

```java
package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.ai.GeneratedQuestion;
import com.albertoreal.pokemongame.ai.QuizGenerator;
import com.albertoreal.pokemongame.game.DailyPokemon;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
import com.albertoreal.pokemongame.game.PokemonNameCatalog;
import com.albertoreal.pokemongame.game.PokemonSelector;
import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QuizOrchestrator.class);

    private final DailyPokemonRepository pokemonRepo;
    private final DailyQuizRepository quizRepo;
    private final DailyQuizQuestionRepository questionRepo;
    private final PokemonSelector selector;
    private final PokeApiClient pokeApi;
    private final QuizGenerator quizGenerator;
    private final PokemonNameCatalog catalog;

    public QuizOrchestrator(DailyPokemonRepository pokemonRepo,
                            DailyQuizRepository quizRepo,
                            DailyQuizQuestionRepository questionRepo,
                            PokemonSelector selector,
                            PokeApiClient pokeApi,
                            QuizGenerator quizGenerator,
                            PokemonNameCatalog catalog) {
        this.pokemonRepo = pokemonRepo;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.selector = selector;
        this.pokeApi = pokeApi;
        this.quizGenerator = quizGenerator;
        this.catalog = catalog;
    }

    @Transactional
    public void ensureExists(LocalDate date) {
        if (quizRepo.findById(date).isPresent()) return;
        try {
            generateForDate(date);
        } catch (DataIntegrityViolationException conflict) {
            log.info("Concurrent generation detected for {}, skipping", date);
        }
    }

    protected void generateForDate(LocalDate date) {
        var used = pokemonRepo.findAll().stream()
            .map(DailyPokemon::getPokemonId)
            .collect(Collectors.toUnmodifiableSet());
        int pokemonId = selector.pick(used);

        PokemonDto pokemon = pokeApi.getPokemon(pokemonId);
        PokemonSpeciesDto species = pokeApi.getSpecies(pokemonId);
        String nameEs = species.nameInLanguage("es");
        if (nameEs == null) nameEs = pokemon.name();
        String imageUrl = pokemon.officialArtworkUrl();
        if (imageUrl == null) imageUrl = "";

        pokemonRepo.save(new DailyPokemon(
            date, pokemonId, pokemon.name(), nameEs, imageUrl, OffsetDateTime.now()));
        catalog.register(pokemon.name());

        var quiz = new DailyQuiz(date, QuizStatus.GENERATING_QUIZ, OffsetDateTime.now());
        quizRepo.save(quiz);

        List<GeneratedQuestion> generated = quizGenerator.generate(pokemon, species);
        for (int i = 0; i < generated.size(); i++) {
            var g = generated.get(i);
            questionRepo.save(new DailyQuizQuestion(
                date, i + 1, g.text(), g.options(), g.correctIndex()));
        }

        quiz.setStatus(QuizStatus.READY);
        quiz.setReadyAt(OffsetDateTime.now());
        quizRepo.save(quiz);
    }
}
```

- [ ] **Step 2: Compilar — aún romperá por QuizView**

```bash
./mvnw -B compile 2>&1 | tail -10
```
Expected: errores solo en `QuizView` y `QuizService` que intentan leer `audioQuestionPath`. Siguientes tasks arreglan.

- [ ] **Step 3: Commit parcial**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/QuizOrchestrator.java
git commit -m "refactor(backend): remove GENERATING_AUDIO phase from QuizOrchestrator"
```

---

## Task 7: Actualizar `QuizView` y `QuizService` — quitar `audioPath`

**Files:**
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/QuizView.java`
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/QuizService.java`

- [ ] **Step 1: `QuizView.java` — sin campo audioPath**

```java
package com.albertoreal.pokemongame.quiz;

import java.util.List;

public record QuizView(String status, List<QuestionView> questions) {

    public record QuestionView(Long id, int position, String text, List<String> options) {}
}
```

- [ ] **Step 2: `QuizService.todayQuiz()` — quitar el audioPath del mapping**

Abre `QuizService.java`, localiza el método `todayQuiz()` y reemplaza el mapping por:

```java
var questions = questionRepo.findByQuizDateOrderByPositionAsc(date).stream()
    .map(q -> new QuizView.QuestionView(
        q.getId(), q.getPosition(), q.getQuestionText(), q.getOptions()))
    .toList();
```

- [ ] **Step 3: Compilar**

```bash
./mvnw -B compile 2>&1 | tail -10
```
Expected: errores solo en `AudioController`, `AudioStorage`, y referencias a `TextToSpeechService`. Task 8 las elimina.

- [ ] **Step 4: Commit parcial**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/main/java/com/albertoreal/pokemongame/quiz/
git commit -m "refactor(backend): remove audioPath from QuizView and QuizService"
```

---

## Task 8: Eliminar todo el paquete `media` y la config de audio

**Files:**
- Delete: `apps/backend/src/main/java/com/albertoreal/pokemongame/media/` (entire directory)
- Delete: `apps/backend/src/test/java/com/albertoreal/pokemongame/media/` (entire directory)
- Modify: `apps/backend/src/main/resources/application.properties` (remove storage property)
- Modify: `apps/backend/.gitignore` or repo `.gitignore` (storage/ rule already exists in root .gitignore — keep it)

- [ ] **Step 1: Eliminar archivos**

```bash
cd /home/fenix/Documents/develop/pokemon-game
rm -rf apps/backend/src/main/java/com/albertoreal/pokemongame/media
rm -rf apps/backend/src/test/java/com/albertoreal/pokemongame/media
```

- [ ] **Step 2: Editar `application.properties` — eliminar la sección Media storage**

Abre `apps/backend/src/main/resources/application.properties` y borra:
```properties

# Media storage
pokemon-game.storage.audio-dir=./storage/audio
```

- [ ] **Step 3: Compilar**

```bash
cd apps/backend
./mvnw -B compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`. Si sobra alguna importación rota en otros archivos (por ejemplo tests que usaban `AudioStorage`), **ir a esos archivos y eliminarlos si eran integration tests que ya no aplican, o eliminar solo las líneas rotas**. Buscar referencias:
```bash
grep -rn "TextToSpeechService\|AudioStorage\|MediaConfig\|AudioController\|StubTtsService\|storage.audio-dir" apps/backend/src/ || true
```
Si aparecen resultados, limpiarlos uno a uno.

En concreto, **`QuizOrchestratorIT`** tiene un `@TempDir audioDir` que ya no necesita. Editar el test:
- Remover `@TempDir static Path audioDir;`
- Remover `r.add("pokemon-game.storage.audio-dir", audioDir::toString);`
- Remover el assert `assertThat(...audioQuestionPath).isNotNull();`

**`GameControllerIT`** y **`EndToEndGameIT`**: eliminar el `@TempDir audioDir` y la propiedad `pokemon-game.storage.audio-dir`.

- [ ] **Step 4: Correr todos los tests para ver qué queda**

```bash
./mvnw -B test 2>&1 | tail -40
```
Expected: **la mayoría verde**, pero algunos fallan porque dependen del antiguo shape de `QuestionView` o del audio. Fixearlos inline.

Si un test menciona `getAudioQuestionPath` o `getAudioOptionsPaths`, eliminar esa aserción.

- [ ] **Step 5: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/
git commit -m "refactor(backend): remove TTS/audio module entirely (browser handles TTS)"
```

---

## Task 9: Extender `PokeApiClient` + DTOs para datos RAG

**Files:**
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/pokeapi/dto/PokemonDto.java`
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/pokeapi/dto/EvolutionChainDto.java`
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/pokeapi/dto/PokemonSpeciesDto.java`
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/pokeapi/PokeApiClient.java`

- [ ] **Step 1: Extender `PokemonDto` con types, height, weight, abilities**

Reemplazar `PokemonDto.java` entero:

```java
package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
    int id,
    String name,
    int height,   // in decimetres (PokeAPI convention)
    int weight,   // in hectograms
    Sprites sprites,
    List<TypeSlot> types,
    List<AbilitySlot> abilities
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(Other other) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Other(@JsonProperty("official-artwork") OfficialArtwork officialArtwork) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record OfficialArtwork(@JsonProperty("front_default") String frontDefault) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeSlot(int slot, Type type) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Type(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(Ability ability, @JsonProperty("is_hidden") boolean isHidden) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ability(String name) {}

    public String officialArtworkUrl() {
        return sprites != null && sprites.other() != null
                && sprites.other().officialArtwork() != null
            ? sprites.other().officialArtwork().frontDefault() : null;
    }

    public double heightMeters() { return height / 10.0; }
    public double weightKilograms() { return weight / 10.0; }

    public List<String> typeNames() {
        return types == null ? List.of()
            : types.stream().map(t -> t.type() == null ? null : t.type().name())
                .filter(java.util.Objects::nonNull).toList();
    }

    public List<String> abilityNames() {
        return abilities == null ? List.of()
            : abilities.stream()
                .filter(a -> a.ability() != null)
                .map(a -> a.ability().name()).toList();
    }
}
```

- [ ] **Step 2: Extender `PokemonSpeciesDto` con generation + evolution_chain URL**

Reemplazar el archivo:

```java
package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesDto(
    int id,
    List<Name> names,
    Generation generation,
    @JsonProperty("evolution_chain") EvolutionChainRef evolutionChain
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Name(String name, Language language) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Language(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Generation(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvolutionChainRef(String url) {}

    public String nameInLanguage(String lang) {
        if (names == null) return null;
        return names.stream()
            .filter(n -> n.language() != null && lang.equals(n.language().name()))
            .map(Name::name)
            .findFirst()
            .orElse(null);
    }

    public String generationName() {
        return generation == null ? null : generation.name();
    }

    public String evolutionChainUrl() {
        return evolutionChain == null ? null : evolutionChain.url();
    }
}
```

- [ ] **Step 3: Crear `EvolutionChainDto.java`**

```java
package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainDto(int id, ChainLink chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(Species species, List<ChainLink> evolves_to) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Species(String name) {}

    /** Returns the evolution chain as a flat list of species names (pre-order). */
    public List<String> evolutionLine() {
        var result = new ArrayList<String>();
        if (chain != null) collect(chain, result);
        return result;
    }

    private static void collect(ChainLink link, List<String> out) {
        if (link.species() != null) out.add(link.species().name());
        if (link.evolves_to() != null) {
            for (ChainLink next : link.evolves_to()) collect(next, out);
        }
    }
}
```

- [ ] **Step 4: Extender `PokeApiClient` con `getEvolutionChain(url)`**

Reemplazar el archivo:

```java
package com.albertoreal.pokemongame.pokeapi;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PokeApiClient {

    private final RestClient client;
    private final RestClient rawClient;
    private final ConcurrentMap<Integer, PokemonDto> pokemonCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, PokemonSpeciesDto> speciesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, EvolutionChainDto> chainCache = new ConcurrentHashMap<>();

    public PokeApiClient(PokeApiConfig config) {
        this.client = RestClient.builder().baseUrl(config.baseUrl()).build();
        this.rawClient = RestClient.builder().build();
    }

    public PokemonDto getPokemon(int id) {
        return pokemonCache.computeIfAbsent(id, i ->
            client.get().uri("/pokemon/{id}", i).retrieve().body(PokemonDto.class));
    }

    public PokemonSpeciesDto getSpecies(int id) {
        return speciesCache.computeIfAbsent(id, i ->
            client.get().uri("/pokemon-species/{id}", i).retrieve().body(PokemonSpeciesDto.class));
    }

    public EvolutionChainDto getEvolutionChain(String absoluteUrl) {
        return chainCache.computeIfAbsent(absoluteUrl, url ->
            rawClient.get().uri(URI.create(url)).retrieve().body(EvolutionChainDto.class));
    }
}
```

- [ ] **Step 5: Actualizar el test `PokeApiClientTest`**

El test existente usa la constructora antigua de `PokemonDto`. Actualizar los casos:

```java
@Test
void dtoExtractsOfficialArtworkUrl() {
    var pokemon = new PokemonDto(25, "pikachu", 4, 60,
        new PokemonDto.Sprites(
            new PokemonDto.Sprites.Other(
                new PokemonDto.Sprites.OfficialArtwork("http://img/pikachu.png"))),
        java.util.List.of(), java.util.List.of());
    assertThat(pokemon.officialArtworkUrl()).isEqualTo("http://img/pikachu.png");
    assertThat(pokemon.heightMeters()).isEqualTo(0.4);
    assertThat(pokemon.weightKilograms()).isEqualTo(6.0);
}

@Test
void dtoReturnsNullWhenSpritesMissing() {
    var pokemon = new PokemonDto(25, "pikachu", 4, 60, null,
        java.util.List.of(), java.util.List.of());
    assertThat(pokemon.officialArtworkUrl()).isNull();
}

@Test
void extractsTypeAndAbilityNames() {
    var pokemon = new PokemonDto(25, "pikachu", 4, 60, null,
        java.util.List.of(new PokemonDto.TypeSlot(1, new PokemonDto.Type("electric"))),
        java.util.List.of(new PokemonDto.AbilitySlot(new PokemonDto.Ability("static"), false)));
    assertThat(pokemon.typeNames()).containsExactly("electric");
    assertThat(pokemon.abilityNames()).containsExactly("static");
}

@Test
void speciesDtoFindsNameInLanguage() {
    var species = new PokemonSpeciesDto(25, java.util.List.of(
        new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("en")),
        new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))
    ), null, null);
    assertThat(species.nameInLanguage("es")).isEqualTo("Pikachu");
    assertThat(species.nameInLanguage("fr")).isNull();
}

@Test
void speciesDtoHandlesNullNames() {
    var species = new PokemonSpeciesDto(25, null, null, null);
    assertThat(species.nameInLanguage("es")).isNull();
}

@Test
void evolutionChainExtractsLine() {
    var chain = new EvolutionChainDto(1,
        new EvolutionChainDto.ChainLink(
            new EvolutionChainDto.Species("pichu"),
            java.util.List.of(new EvolutionChainDto.ChainLink(
                new EvolutionChainDto.Species("pikachu"),
                java.util.List.of(new EvolutionChainDto.ChainLink(
                    new EvolutionChainDto.Species("raichu"),
                    java.util.List.of()))))));
    assertThat(chain.evolutionLine()).containsExactly("pichu", "pikachu", "raichu");
}
```

Añade el import:
```java
import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
```

- [ ] **Step 6: Verificar también **otros tests que construían PokemonDto**: `StubQuizGeneratorTest`, `QuizOrchestratorIT`, `GameControllerIT`, `EndToEndGameIT`

Buscar usages:
```bash
cd /home/fenix/Documents/develop/pokemon-game
grep -rn "new PokemonDto(" apps/backend/src/test/ | head
```

Para cada uno, cambiar la constructora a la nueva forma (con `height`, `weight`, `types`, `abilities`). Para los tests que no usan esos campos, usar `0, 0, null, List.of(), List.of()` como valores dummy.

- [ ] **Step 7: Ejecutar tests**

```bash
cd apps/backend
./mvnw -B test 2>&1 | tail -20
```
Expected: `Tests run: N, Failures: 0` + `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): extend PokeAPI DTOs and client for RAG context (types, abilities, evolution)"
```

---

## Task 10: Record `GeneratedQuiz` (wrapper para salida estructurada del LLM)

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/GeneratedQuiz.java`

- [ ] **Step 1: Crear el record**

```java
package com.albertoreal.pokemongame.ai;

import java.util.List;

/** Wrapper used as the structured-output target for LLM quiz generation. */
public record GeneratedQuiz(List<GeneratedQuestion> questions) {}
```

- [ ] **Step 2: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): add GeneratedQuiz record for structured LLM output"
```

---

## Task 11: `RagContextBuilder` — convierte DTOs de PokeAPI en contexto para el prompt

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/RagContextBuilder.java`
- Create: `apps/backend/src/test/java/com/albertoreal/pokemongame/ai/RagContextBuilderTest.java`

- [ ] **Step 1: Test failing**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextBuilderTest {

    @Test
    void buildsContextWithAllFacts() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60, null,
            List.of(new PokemonDto.TypeSlot(1, new PokemonDto.Type("electric"))),
            List.of(new PokemonDto.AbilitySlot(new PokemonDto.Ability("static"), false)));
        var species = new PokemonSpeciesDto(25, List.of(
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))),
            new PokemonSpeciesDto.Generation("generation-i"), null);
        var chain = new EvolutionChainDto(1, new EvolutionChainDto.ChainLink(
            new EvolutionChainDto.Species("pichu"),
            List.of(new EvolutionChainDto.ChainLink(
                new EvolutionChainDto.Species("pikachu"),
                List.of(new EvolutionChainDto.ChainLink(
                    new EvolutionChainDto.Species("raichu"),
                    List.of()))))));

        var ctx = new RagContextBuilder().build(pokemon, species, chain);

        assertThat(ctx).contains("Pikachu");
        assertThat(ctx).contains("electric");
        assertThat(ctx).contains("static");
        assertThat(ctx).contains("0.4");
        assertThat(ctx).contains("6.0");
        assertThat(ctx).contains("generation-i");
        assertThat(ctx).contains("pichu");
        assertThat(ctx).contains("raichu");
    }

    @Test
    void handlesNullEvolutionChain() {
        var pokemon = new PokemonDto(132, "ditto", 3, 40, null,
            List.of(), List.of());
        var species = new PokemonSpeciesDto(132,
            List.of(new PokemonSpeciesDto.Name("Ditto", new PokemonSpeciesDto.Language("es"))),
            null, null);

        var ctx = new RagContextBuilder().build(pokemon, species, null);
        assertThat(ctx).contains("Ditto");
    }
}
```

Run: expected FAIL (class missing).

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
./mvnw -B test -Dtest=RagContextBuilderTest 2>&1 | tail -10
```

- [ ] **Step 2: Implementación**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RagContextBuilder {

    public String build(PokemonDto pokemon, PokemonSpeciesDto species, EvolutionChainDto chain) {
        String nameEs = species != null ? species.nameInLanguage("es") : null;
        if (nameEs == null) nameEs = pokemon.name();
        StringBuilder sb = new StringBuilder();
        sb.append("Nombre en castellano: ").append(nameEs).append('\n');
        sb.append("Nombre en inglés: ").append(pokemon.name()).append('\n');
        sb.append("ID PokeAPI: ").append(pokemon.id()).append('\n');
        sb.append(String.format(Locale.US, "Altura: %.1f m%n", pokemon.heightMeters()));
        sb.append(String.format(Locale.US, "Peso: %.1f kg%n", pokemon.weightKilograms()));
        sb.append("Tipos: ").append(String.join(", ", pokemon.typeNames())).append('\n');
        sb.append("Habilidades: ").append(String.join(", ", pokemon.abilityNames())).append('\n');
        if (species != null && species.generationName() != null) {
            sb.append("Generación: ").append(species.generationName()).append('\n');
        }
        if (chain != null) {
            sb.append("Cadena evolutiva (en orden): ")
              .append(String.join(" → ", chain.evolutionLine())).append('\n');
        }
        return sb.toString();
    }
}
```

- [ ] **Step 3: Run test — PASS**

```bash
./mvnw -B test -Dtest=RagContextBuilderTest 2>&1 | tail -10
```
Expected: `Tests run: 2, Failures: 0` + `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): add RagContextBuilder for structured LLM prompt context"
```

---

## Task 12: Configuración Spring AI — tres proveedores como beans

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/AiProvidersConfig.java`
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/AiProvidersProperties.java`
- Modify: `apps/backend/src/main/resources/application.properties`

- [ ] **Step 1: Propiedades**

```java
package com.albertoreal.pokemongame.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-game.ai")
public record AiProvidersProperties(
    String provider,               // "stub" (default) | "chain"
    Ollama ollama,
    Groq groq,
    OpenRouter openrouter
) {
    public AiProvidersProperties {
        if (provider == null || provider.isBlank()) provider = "stub";
        if (ollama == null) ollama = new Ollama("http://localhost:11434", "llama3.2:3b");
        if (groq == null) groq = new Groq(null, "llama-3.3-70b-versatile", false);
        if (openrouter == null) openrouter = new OpenRouter(null,
            "meta-llama/llama-3.3-70b-instruct:free", false);
    }

    public record Ollama(String baseUrl, String model) {
        public Ollama {
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
            if (model == null || model.isBlank()) model = "llama3.2:3b";
        }
    }

    public record Groq(String apiKey, String model, boolean enabled) {
        public Groq {
            if (model == null || model.isBlank()) model = "llama-3.3-70b-versatile";
        }
    }

    public record OpenRouter(String apiKey, String model, boolean enabled) {
        public OpenRouter {
            if (model == null || model.isBlank()) model = "meta-llama/llama-3.3-70b-instruct:free";
        }
    }
}
```

- [ ] **Step 2: `AiProvidersConfig` — crea los ChatModel beans**

Importante: Spring AI 1.x tiene auto-config que, con los starters, creará un `OllamaChatModel` y un `OpenAiChatModel` automáticamente. Para tener DOS OpenAI clients (Groq + OpenRouter), necesitamos excluir o añadir beans manuales con `@Qualifier`. Estrategia: dejamos el auto-config del OpenAI **desactivado** (el `spring.ai.openai.api-key` vacío por defecto) y montamos ambos manualmente.

```java
package com.albertoreal.pokemongame.ai;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProvidersConfig {

    @Bean(name = "groqChatModel")
    public OpenAiChatModel groqChatModel(AiProvidersProperties props) {
        if (!props.groq().enabled()) return null;
        var api = OpenAiApi.builder()
            .baseUrl("https://api.groq.com/openai")
            .apiKey(props.groq().apiKey())
            .build();
        var options = OpenAiChatOptions.builder()
            .model(props.groq().model())
            .temperature(0.2)
            .build();
        return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
    }

    @Bean(name = "openRouterChatModel")
    public OpenAiChatModel openRouterChatModel(AiProvidersProperties props) {
        if (!props.openrouter().enabled()) return null;
        var api = OpenAiApi.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .apiKey(props.openrouter().apiKey())
            .build();
        var options = OpenAiChatOptions.builder()
            .model(props.openrouter().model())
            .temperature(0.2)
            .build();
        return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
    }
}
```

> **Nota**: la API builder de `OpenAiApi` y `OpenAiChatModel` puede variar ligeramente entre versiones 1.0.x de Spring AI. Si `OpenAiApi.builder()` no existe, usar el constructor `new OpenAiApi(baseUrl, apiKey)`. Consultar <https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html> o vía context7 MCP. Compilar incremental hasta que funcione.

- [ ] **Step 3: Añadir a `application.properties`**

```properties

# AI providers
pokemon-game.ai.provider=stub
pokemon-game.ai.ollama.base-url=http://localhost:11434
pokemon-game.ai.ollama.model=llama3.2:3b
pokemon-game.ai.groq.enabled=false
pokemon-game.ai.groq.api-key=${GROQ_API_KEY:}
pokemon-game.ai.groq.model=llama-3.3-70b-versatile
pokemon-game.ai.openrouter.enabled=false
pokemon-game.ai.openrouter.api-key=${OPENROUTER_API_KEY:}
pokemon-game.ai.openrouter.model=meta-llama/llama-3.3-70b-instruct:free

# Disable Spring AI auto-config that requires openai.api-key
spring.ai.openai.api-key=noop
spring.ai.ollama.init.chat.include=false
```

- [ ] **Step 4: Compile**

```bash
cd apps/backend
./mvnw -B compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`. Si falla por el API builder, ajustar el constructor/builder hasta que compile.

- [ ] **Step 5: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/ apps/backend/src/main/resources/application.properties
git commit -m "feat(backend): add Spring AI config with Ollama, Groq and OpenRouter beans"
```

---

## Task 13: `OllamaQuizGenerator` — implementación con Spring AI

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/OllamaQuizGenerator.java`
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/QuizPrompt.java`

- [ ] **Step 1: `QuizPrompt` — template compartido para todos los proveedores**

```java
package com.albertoreal.pokemongame.ai;

public final class QuizPrompt {

    private QuizPrompt() {}

    public static final String SYSTEM = """
        Eres un generador de quizzes en castellano sobre Pokemon. Generas exactamente
        5 preguntas de opción múltiple (4 opciones cada una) basándote ÚNICAMENTE en los
        datos factuales proporcionados en el contexto. NO uses conocimiento externo.
        Las preguntas deben ser variadas y cubrir: tipo, generación/región, altura/peso,
        habilidades y evoluciones. Todas las opciones deben ser plausibles y en castellano.

        Responde con un objeto JSON CON ESTA FORMA EXACTA y NADA MÁS, sin explicaciones:
        {
          "questions": [
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 0},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 1},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 2},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 3},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 0}
          ]
        }

        correctIndex es un entero 0-3. Genera EXACTAMENTE 5 preguntas, ni más ni menos.
        """;

    public static String user(String ragContext) {
        return "Datos del Pokemon:\n\n" + ragContext + "\nGenera el quiz ahora.";
    }
}
```

- [ ] **Step 2: `OllamaQuizGenerator`**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("ollamaQuizGenerator")
public class OllamaQuizGenerator implements QuizGenerator {

    private final ChatClient client;
    private final PokeApiClient pokeApi;
    private final RagContextBuilder rag;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaQuizGenerator(OllamaChatModel ollama,
                               PokeApiClient pokeApi,
                               RagContextBuilder rag) {
        this.client = ChatClient.create(ollama);
        this.pokeApi = pokeApi;
        this.rag = rag;
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        String chainLine = species != null && species.evolutionChainUrl() != null
            ? pokeApi.getEvolutionChain(species.evolutionChainUrl()).evolutionLine().toString()
            : "";
        var chain = species != null && species.evolutionChainUrl() != null
            ? pokeApi.getEvolutionChain(species.evolutionChainUrl())
            : null;
        String ragCtx = rag.build(pokemon, species, chain);

        String json = client.prompt()
            .system(QuizPrompt.SYSTEM)
            .user(QuizPrompt.user(ragCtx))
            .call()
            .content();

        try {
            GeneratedQuiz quiz = mapper.readValue(json, GeneratedQuiz.class);
            if (quiz.questions() == null || quiz.questions().size() != 5) {
                throw new QuizGenerationException(
                    "Expected 5 questions, got " + (quiz.questions() == null ? 0 : quiz.questions().size()));
            }
            return quiz.questions();
        } catch (Exception e) {
            throw new QuizGenerationException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 3: Excepción dedicada**

```java
// apps/backend/src/main/java/com/albertoreal/pokemongame/ai/QuizGenerationException.java
package com.albertoreal.pokemongame.ai;

public class QuizGenerationException extends RuntimeException {
    public QuizGenerationException(String msg) { super(msg); }
    public QuizGenerationException(String msg, Throwable t) { super(msg, t); }
}
```

- [ ] **Step 4: Compile**

```bash
./mvnw -B compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`. Si Spring AI's `ChatClient.create(ollama)` no existe, usar `ChatClient.builder(ollama).build()`.

- [ ] **Step 5: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): add OllamaQuizGenerator with RAG prompt and JSON output"
```

---

## Task 14: `GroqQuizGenerator` + `OpenRouterQuizGenerator`

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/GroqQuizGenerator.java`
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/OpenRouterQuizGenerator.java`

Patrón: iguales al Ollama pero con su propio `OpenAiChatModel` bean.

- [ ] **Step 1: `GroqQuizGenerator`**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("groqQuizGenerator")
@ConditionalOnBean(name = "groqChatModel")
public class GroqQuizGenerator implements QuizGenerator {

    private final ChatClient client;
    private final PokeApiClient pokeApi;
    private final RagContextBuilder rag;
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqQuizGenerator(@Qualifier("groqChatModel") OpenAiChatModel groq,
                             PokeApiClient pokeApi,
                             RagContextBuilder rag) {
        this.client = ChatClient.create(groq);
        this.pokeApi = pokeApi;
        this.rag = rag;
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        var chain = species != null && species.evolutionChainUrl() != null
            ? pokeApi.getEvolutionChain(species.evolutionChainUrl())
            : null;
        String ragCtx = rag.build(pokemon, species, chain);

        String json = client.prompt()
            .system(QuizPrompt.SYSTEM)
            .user(QuizPrompt.user(ragCtx))
            .call()
            .content();

        try {
            GeneratedQuiz quiz = mapper.readValue(json, GeneratedQuiz.class);
            if (quiz.questions() == null || quiz.questions().size() != 5) {
                throw new QuizGenerationException(
                    "Expected 5 questions, got " + (quiz.questions() == null ? 0 : quiz.questions().size()));
            }
            return quiz.questions();
        } catch (Exception e) {
            throw new QuizGenerationException("Failed to parse Groq response: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 2: `OpenRouterQuizGenerator` — igual pero con su qualifier**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("openRouterQuizGenerator")
@ConditionalOnBean(name = "openRouterChatModel")
public class OpenRouterQuizGenerator implements QuizGenerator {

    private final ChatClient client;
    private final PokeApiClient pokeApi;
    private final RagContextBuilder rag;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouterQuizGenerator(@Qualifier("openRouterChatModel") OpenAiChatModel openRouter,
                                   PokeApiClient pokeApi,
                                   RagContextBuilder rag) {
        this.client = ChatClient.create(openRouter);
        this.pokeApi = pokeApi;
        this.rag = rag;
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        var chain = species != null && species.evolutionChainUrl() != null
            ? pokeApi.getEvolutionChain(species.evolutionChainUrl())
            : null;
        String ragCtx = rag.build(pokemon, species, chain);

        String json = client.prompt()
            .system(QuizPrompt.SYSTEM)
            .user(QuizPrompt.user(ragCtx))
            .call()
            .content();

        try {
            GeneratedQuiz quiz = mapper.readValue(json, GeneratedQuiz.class);
            if (quiz.questions() == null || quiz.questions().size() != 5) {
                throw new QuizGenerationException(
                    "Expected 5 questions, got " + (quiz.questions() == null ? 0 : quiz.questions().size()));
            }
            return quiz.questions();
        } catch (Exception e) {
            throw new QuizGenerationException("Failed to parse OpenRouter response: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 3: Compile**

```bash
./mvnw -B compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): add Groq and OpenRouter quiz generators"
```

---

## Task 15: `ChainQuizGenerator` — fallback entre proveedores

**Files:**
- Create: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/ChainQuizGenerator.java`
- Modify: `apps/backend/src/main/java/com/albertoreal/pokemongame/ai/StubQuizGenerator.java` (ajustar `@Profile`)

- [ ] **Step 1: `ChainQuizGenerator` como `@Primary` si se activa**

```java
package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
@ConditionalOnProperty(prefix = "pokemon-game.ai", name = "provider", havingValue = "chain")
public class ChainQuizGenerator implements QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(ChainQuizGenerator.class);

    private final List<QuizGenerator> providers;

    public ChainQuizGenerator(OllamaQuizGenerator ollama,
                              ObjectProvider<GroqQuizGenerator> groq,
                              ObjectProvider<OpenRouterQuizGenerator> openRouter) {
        this.providers = new ArrayList<>();
        providers.add(ollama);
        GroqQuizGenerator g = groq.getIfAvailable();
        if (g != null) providers.add(g);
        OpenRouterQuizGenerator or = openRouter.getIfAvailable();
        if (or != null) providers.add(or);
        log.info("ChainQuizGenerator initialized with {} providers", providers.size());
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        QuizGenerationException last = null;
        for (QuizGenerator provider : providers) {
            try {
                log.info("Trying provider {}", provider.getClass().getSimpleName());
                return provider.generate(pokemon, species);
            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.getClass().getSimpleName(), e.getMessage());
                last = (e instanceof QuizGenerationException qge) ? qge
                    : new QuizGenerationException(e.getMessage(), e);
            }
        }
        throw last != null ? last
            : new QuizGenerationException("No AI provider configured");
    }
}
```

- [ ] **Step 2: Hacer `StubQuizGenerator` condicional al perfil O a la ausencia de `provider=chain`**

Abre `StubQuizGenerator.java` y reemplaza `@Profile("!ai-real")` por:

```java
@Component
@ConditionalOnProperty(prefix = "pokemon-game.ai", name = "provider",
                       havingValue = "stub", matchIfMissing = true)
public class StubQuizGenerator implements QuizGenerator {
    // ... resto igual
}
```

(Añadir el import `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` y quitar `@Profile`.)

- [ ] **Step 3: Compile + unit tests pasando (no se ejecutan las chains reales aún)**

```bash
./mvnw -B test 2>&1 | tail -15
```
Expected: `Tests run: N, Failures: 0` + `BUILD SUCCESS`. Los tests existentes usan `StubQuizGenerator` porque la propiedad por defecto es `stub`.

- [ ] **Step 4: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/
git commit -m "feat(backend): add ChainQuizGenerator with fallback across AI providers"
```

---

## Task 16: Smoke test manual contra Ollama real

Esta task NO produce código — valida que con `pokemon-game.ai.provider=chain` Ollama responde.

**Files:** ninguno.

- [ ] **Step 1: Comprobar que Ollama está arriba y el modelo pulleado**

```bash
docker exec pokemon-game-ollama ollama list | grep llama3.2
curl -s http://localhost:11434/api/tags | jq '.models[].name'
```

- [ ] **Step 2: Arrancar el backend con perfil `local` y `provider=chain`**

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
export PATH=$JAVA_HOME/bin:$PATH
SPRING_PROFILES_ACTIVE=local POKEMON_GAME_AI_PROVIDER=chain ./mvnw -B spring-boot:run \
  > /tmp/spring-ai.log 2>&1 &
SPRING_PID=$!
```

Espera ~30 segundos. El `DailyQuizStartupRunner` va a invocar `orchestrator.ensureExists(today)` y, si es la primera vez hoy, llamará a PokeAPI + Ollama. Esperar hasta 2-3 min más porque la inferencia LLM puede tardar.

Seguir los logs:
```bash
tail -f /tmp/spring-ai.log | grep -E "ChainQuizGenerator|Ollama|generate|READY|FAILED"
```
Expected: línea `Trying provider OllamaQuizGenerator` + después `Started PokemonGameBackendApplication` + eventualmente quiz en estado `READY` en DB.

- [ ] **Step 3: Verificar el quiz del día en DB**

```bash
docker exec pokemon-game-postgres psql -U pokemon -d pokemon_game \
  -c "SELECT position, question_text, options, correct_option_index
      FROM daily_quiz_question WHERE quiz_date = CURRENT_DATE ORDER BY position;"
```
Expected: 5 filas con preguntas reales generadas por Ollama (en castellano, sobre el Pokemon del día).

- [ ] **Step 4: Probar el flujo completo**

```bash
curl -s http://localhost:8080/api/game/today | jq
curl -s http://localhost:8080/api/game/today/quiz | jq
```

- [ ] **Step 5: Parar Spring**

```bash
kill $SPRING_PID 2>/dev/null
pkill -f spring-boot:run 2>/dev/null
```

- [ ] **Step 6: Si todo bien, commit de un simple marker file o skip este paso**

No hay que commitear nada — es una validación manual. Si las preguntas del quiz pintan bien (coherentes con el Pokemon, en castellano, 4 opciones cada una), cierra esta task.

Si las preguntas NO son coherentes (por ejemplo el modelo inventa datos), es un indicio de que el prompt o el modelo son débiles. Opciones:
- Probar un modelo más grande: `sdk install model qwen2.5:7b` + cambiar `pokemon-game.ai.ollama.model=qwen2.5:7b`.
- Activar Groq en fallback: `POKEMON_GAME_AI_GROQ_ENABLED=true` + `GROQ_API_KEY=... ./mvnw ...`.

---

## Task 17: Activar Groq y OpenRouter con las keys del `.env.local`

**Files:**
- Modify: `apps/backend/src/main/resources/application-local.properties`

- [ ] **Step 1: Añadir a `application-local.properties`**

```properties
pokemon-game.ai.provider=chain
pokemon-game.ai.groq.enabled=true
pokemon-game.ai.openrouter.enabled=true
```

**IMPORTANTE**: el perfil `local` activa la cadena. Las API keys se leen de variables de entorno (`GROQ_API_KEY`, `OPENROUTER_API_KEY`) que viven en `.env.local` (gitignored). El usuario las exporta antes de arrancar o usa un plugin del IDE.

- [ ] **Step 2: Verificar cargando `.env.local`**

Arrancar con las claves desde el archivo:

```bash
cd /home/fenix/Documents/develop/pokemon-game/apps/backend
set -a && source ../../.env.local && set +a
export JAVA_HOME=/home/fenix/.sdkman/candidates/java/25.0.2-graalce
export PATH=$JAVA_HOME/bin:$PATH
SPRING_PROFILES_ACTIVE=local ./mvnw -B spring-boot:run > /tmp/spring-keys.log 2>&1 &
SPRING_PID=$!

sleep 20
grep -E "ChainQuizGenerator initialized|groq|openrouter" /tmp/spring-keys.log | head -5
```
Expected: `ChainQuizGenerator initialized with 3 providers`.

Parar el backend:
```bash
kill $SPRING_PID 2>/dev/null
pkill -f spring-boot:run 2>/dev/null
```

- [ ] **Step 3: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/src/main/resources/application-local.properties
git commit -m "feat(backend): enable AI chain (Ollama + Groq + OpenRouter) in local profile"
```

---

## Task 18: Actualizar tests afectados y correr suite completa

**Files:**
- Audit and fix any remaining test that uses the removed types.

- [ ] **Step 1: Buscar referencias rotas**

```bash
cd /home/fenix/Documents/develop/pokemon-game
grep -rn "TextToSpeech\|AudioStorage\|audioPath\|StubTts\|audio_question_path\|audio_options_paths\|MediaConfig" apps/backend/src/ || echo "clean"
```

Si `clean`, seguir. Si aparecen referencias, eliminarlas (suelen ser imports huérfanos o aserciones de tests).

- [ ] **Step 2: Correr toda la suite de tests**

```bash
cd apps/backend
./mvnw -B test 2>&1 | tail -15
```
Expected: `Tests run: N, Failures: 0` + `BUILD SUCCESS`.

Si algún test falla por un edge case relacionado con los cambios, arreglarlo **solo lo necesario** sin scope-creep.

- [ ] **Step 3: Commit si hubo fixes**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/
git commit -m "test(backend): fix tests after TTS/audio removal" || echo "nothing to commit"
```

---

## Task 19: Actualizar README del backend

**Files:**
- Modify: `apps/backend/README.md`

- [ ] **Step 1: Reescribir `apps/backend/README.md` con el estado post-Plan-2**

```markdown
# pokemon-game — backend

Spring Boot 3.5 + Java 25 backend for the daily Pokemon guessing game with real AI quiz generation.

## Stack

- Spring Boot 3.5.5 + Java 25 (GraalVM CE, pinned in `.sdkmanrc`)
- Postgres 17 via Docker Compose
- Ollama (llama3.2:3b by default) for on-prem LLM
- Spring AI 1.x — Ollama primary, Groq and OpenRouter fallbacks
- Flyway for DB migrations, Testcontainers for integration tests
- No server-side audio. The browser does TTS via `speechSynthesis` (see `apps/frontend/`).

## Prerequisites

1. SDKMAN with `java 25.0.2-graalce` installed:
   ```bash
   sdk install java 25.0.2-graalce
   ```
2. Docker running (Postgres + Ollama + Testcontainers).
3. Optional: `GROQ_API_KEY` and/or `OPENROUTER_API_KEY` in `../../.env.local` for fallback tiers. Ollama alone works without any API key.

## Run locally

From the repo root:

```bash
docker compose -f infra/docker-compose.yml up -d

cd apps/backend
sdk env                       # activates 25.0.2-graalce
set -a && source ../../.env.local && set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The `local` profile enables the AI chain (Ollama → Groq → OpenRouter).
Without API keys, only Ollama participates. On startup the `DailyQuizStartupRunner`
generates today's quiz if not present (1 real LLM call against Ollama).

## HTTP endpoints

### Game (user flow)
- `GET  /api/game/today`
- `POST /api/game/today/attempt` — body `{ "transcript": "pikachu" }`
- `POST /api/game/today/surrender`

### Quiz (user flow)
- `GET  /api/game/today/quiz`
- `POST /api/game/today/quiz/answer/{questionId}` — body `{ "transcript": "A" }`

### Admin (stub auth only)
- `POST /api/admin/quiz/generate?date=YYYY-MM-DD`
- `POST /api/admin/quiz/regenerate?date=YYYY-MM-DD`
- `GET  /api/admin/quiz/status?date=YYYY-MM-DD`

### Actuator
- `GET  /actuator/health`
- `GET  /actuator/info`

## Tests

```bash
./mvnw test
```

Unit + integration tests. No network required for Ollama/Groq (providers are
stubbed where tests exercise the orchestrator). First run pulls
`postgres:17-alpine` for Testcontainers.

## Configuration reference

| Property | Default | Purpose |
|---|---|---|
| `pokemon-game.ai.provider` | `stub` | `stub` for fake quizzes or `chain` for real AI |
| `pokemon-game.ai.ollama.base-url` | `http://localhost:11434` | Ollama endpoint |
| `pokemon-game.ai.ollama.model` | `llama3.2:3b` | Model name pulled into Ollama |
| `pokemon-game.ai.groq.enabled` | `false` | Enable Groq in the chain |
| `pokemon-game.ai.groq.api-key` | `${GROQ_API_KEY:}` | Free-tier API key (console.groq.com) |
| `pokemon-game.ai.openrouter.enabled` | `false` | Enable OpenRouter in the chain |
| `pokemon-game.ai.openrouter.api-key` | `${OPENROUTER_API_KEY:}` | API key from openrouter.ai |
| `pokemon-game.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokeAPI endpoint |
| `pokemon-game.auth.stub-user` | `dev` | Stub user id until Keycloak |
| `pokemon-game.scheduler.enabled` | `false` | Enable `@Scheduled` cron |
| `pokemon-game.scheduler.cron` | `0 0 6 * * *` | Cron expression |
| `pokemon-game.scheduler.zone` | `Europe/Madrid` | Time zone |

## Auth

Stub: hardcoded `user_id = "dev"` via `CurrentUser`. Keycloak arrives with OCI migration.

## Known limitations

- `MultipleChoiceMatcher` prioritizes ordinals ("una" → index 0) over literal content. Prefer voice answers with letters (A/B/C/D) for unambiguous intent.
- Today's quiz is generated once per day; regenerate via `POST /api/admin/quiz/regenerate?date=...` to get fresh questions.
- The `daily_pokemon.pokemon_id` random pool has 1025 entries. After ~3 years of daily play it will exhaust.
```

- [ ] **Step 2: Commit**

```bash
cd /home/fenix/Documents/develop/pokemon-game
git add apps/backend/README.md
git commit -m "docs(backend): update README for Plan 2 (real AI, no audio backend)"
```

---

## Self-Review

**1. Spec coverage** — Plan 2 aims:
- [x] Real Ollama generator: Task 13
- [x] Groq fallback: Task 14
- [x] OpenRouter fallback: Task 14
- [x] Fallback orchestration: Task 15
- [x] Remove Google TTS + audio pipeline: Tasks 4, 6, 7, 8
- [x] Update spec: Task 1
- [x] Config for keys from env: Task 12, 17
- [x] Real-run smoke test: Task 16
- [x] Tests still green: Task 18
- [x] README updated: Task 19

**2. Placeholder scan** — No "TBD"/"TODO" in tasks. Intentionally noted points (Spring AI API builder variance, prompt quality) are flagged as runtime decisions for the subagent, not pending work.

**3. Type consistency**:
- `GeneratedQuiz.questions()` → `List<GeneratedQuestion>` used consistently.
- `QuizView.QuestionView` no longer has `audioPath`.
- `DailyQuizQuestion` without audio fields.
- All providers implement the same `QuizGenerator` interface with same return type.

**4. Scope check** — One cohesive sub-project: replace stubs with real AI, remove audio pipeline. No hidden side systems.

No issues requiring fixes.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-23-plan-2-real-ai-integration.md`. Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task + two-stage review.
2. **Inline Execution** — batch with checkpoints.

**Recommended: Subagent-Driven**, continuing from Plan 1's workflow.
