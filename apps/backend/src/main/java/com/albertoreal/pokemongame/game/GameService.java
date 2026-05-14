package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.common.TextNormalizer;
import com.albertoreal.pokemongame.quiz.DailyQuiz;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stateless game logic. The server knows the daily Pokemon and validates
 * each guess; per-session state (attempts, score, solved status) lives in
 * the frontend and is replayed back via {@code previousAttempts} when needed
 * for hint computation.
 */
@Service
public class GameService {

    public static final int MAX_ATTEMPTS = 5;

    private final DailyPokemonRepository pokemonRepo;
    private final DailyQuizRepository quizRepo;
    private final PokemonNameCatalog catalog;

    public GameService(DailyPokemonRepository pokemonRepo,
                       DailyQuizRepository quizRepo,
                       PokemonNameCatalog catalog) {
        this.pokemonRepo = pokemonRepo;
        this.quizRepo = quizRepo;
        this.catalog = catalog;
    }

    public GameState today() {
        var date = LocalDate.now();
        var pokemon = pokemonRepo.findById(date).orElseThrow();
        DailyQuiz quiz = quizRepo.findById(date).orElseThrow();
        String answer = TextNormalizer.normalize(pokemon.getPokemonName());
        return new GameState(
            pokemon.getImageUrl(),
            answer.length(),
            quiz.getStatus() == QuizStatus.READY,
            quiz.getStatus().name(),
            MAX_ATTEMPTS
        );
    }

    public AttemptResult attempt(String transcript, List<NameAttemptView> previousAttempts) {
        var date = LocalDate.now();
        var pokemon = pokemonRepo.findById(date).orElseThrow();
        var prior = previousAttempts == null ? List.<NameAttemptView>of() : previousAttempts;

        if (prior.size() >= MAX_ATTEMPTS) {
            // Defensive: the frontend should not have called us, but if it does
            // (race), report game-over without re-evaluating.
            return gameOverResult(false, prior, pokemon);
        }

        String answer = TextNormalizer.normalize(pokemon.getPokemonName());
        var matcher = new PokemonNameMatcher(catalog.all());
        var matched = matcher.match(transcript);

        boolean correct = matched
            .map(m -> m.equalsIgnoreCase(pokemon.getPokemonName()))
            .orElse(false);

        String guess = matched.orElseGet(() -> TextNormalizer.normalize(transcript));
        String feedback = guess.isEmpty() ? "" : WordleEvaluator.evaluate(guess, answer);
        int attemptsUsedAfter = prior.size() + 1;

        if (correct) {
            int score = MAX_ATTEMPTS - (attemptsUsedAfter - 1);
            return new AttemptResult(
                true,
                guess,
                feedback,
                null,
                pokemon.getPokemonName(),
                pokemon.getPokemonNameEs(),
                score
            );
        }

        // Wrong guess. Build the new running list to compute hints (and to
        // detect game-over if this was the 5th attempt).
        List<NameAttemptView> running = new ArrayList<>(prior);
        if (!guess.isEmpty()) {
            running.add(new NameAttemptView(guess, feedback));
        }
        boolean gameOver = attemptsUsedAfter >= MAX_ATTEMPTS;

        if (gameOver) {
            return new AttemptResult(
                false,
                guess,
                feedback,
                null,
                pokemon.getPokemonName(),
                pokemon.getPokemonNameEs(),
                0
            );
        }

        String hints = calculateHints(answer, running, attemptsUsedAfter);
        return new AttemptResult(false, guess, feedback, hints, null, null, null);
    }

    public SurrenderResult surrender() {
        var pokemon = pokemonRepo.findById(LocalDate.now()).orElseThrow();
        return new SurrenderResult(pokemon.getPokemonName(), pokemon.getPokemonNameEs());
    }

    private AttemptResult gameOverResult(boolean correct, List<NameAttemptView> prior, DailyPokemon pokemon) {
        var last = prior.isEmpty() ? new NameAttemptView("", "") : prior.get(prior.size() - 1);
        return new AttemptResult(
            correct,
            last.guess(),
            last.feedback(),
            null,
            pokemon.getPokemonName(),
            pokemon.getPokemonNameEs(),
            correct ? MAX_ATTEMPTS - (prior.size() - 1) : 0
        );
    }

    private String calculateHints(String answer, List<NameAttemptView> attempts, int attemptsUsed) {
        if (attemptsUsed < 3) return null;

        int n = answer.length();
        char[] hints = new char[n];
        for (int i = 0; i < n; i++) hints[i] = '_';

        Set<Integer> knownCorrectPositions = new HashSet<>();
        Set<Character> knownPresentLetters = new HashSet<>();

        for (var a : attempts) {
            for (int i = 0; i < a.feedback().length(); i++) {
                if (a.feedback().charAt(i) == 'H' && i < n) {
                    knownCorrectPositions.add(i);
                } else if (a.feedback().charAt(i) == 'P') {
                    knownPresentLetters.add(a.guess().charAt(i));
                }
            }
        }

        Set<Character> knownLetters = new HashSet<>(knownPresentLetters);
        for (int pos : knownCorrectPositions) {
            knownLetters.add(answer.charAt(pos));
        }

        List<Integer> availablePositions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!knownCorrectPositions.contains(i)) {
                availablePositions.add(i);
            }
        }

        Collections.shuffle(availablePositions);

        int hintsAdded = 0;
        for (int pos : availablePositions) {
            if (hintsAdded >= 3) break;
            if (knownLetters.size() + hintsAdded >= n - 2) break;

            char c = answer.charAt(pos);
            if (knownLetters.contains(c)) continue;

            hints[pos] = c;
            hintsAdded++;
        }

        if (hintsAdded < 3 && knownLetters.size() + hintsAdded < n - 2) {
            for (int pos : availablePositions) {
                if (hintsAdded >= 3) break;
                if (knownLetters.size() + hintsAdded >= n - 2) break;
                if (hints[pos] != '_') continue;

                hints[pos] = answer.charAt(pos);
                hintsAdded++;
            }
        }

        return new String(hints);
    }
}
