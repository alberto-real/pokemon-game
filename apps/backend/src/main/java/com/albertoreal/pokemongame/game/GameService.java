package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.common.CurrentUser;
import com.albertoreal.pokemongame.common.TextNormalizer;
import com.albertoreal.pokemongame.quiz.DailyQuiz;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GameService {

    static final int MAX_ATTEMPTS = 5;

    private final DailyPokemonRepository pokemonRepo;
    private final DailyQuizRepository quizRepo;
    private final UserDailyAttemptRepository attemptRepo;
    private final PokemonNameCatalog catalog;
    private final CurrentUser currentUser;

    public GameService(DailyPokemonRepository pokemonRepo,
                       DailyQuizRepository quizRepo,
                       UserDailyAttemptRepository attemptRepo,
                       PokemonNameCatalog catalog,
                       CurrentUser currentUser) {
        this.pokemonRepo = pokemonRepo;
        this.quizRepo = quizRepo;
        this.attemptRepo = attemptRepo;
        this.catalog = catalog;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public GameState today() {
        return stateFor(LocalDate.now());
    }

    @Transactional
    public AttemptResult attempt(String transcript) {
        var date = LocalDate.now();
        var pokemon = pokemonRepo.findById(date).orElseThrow();
        var attempt = attemptRepo.findByUserIdAndDate(currentUser.userId(), date)
            .orElseGet(() -> new UserDailyAttempt(currentUser.userId(), date));

        if (attempt.isNameSolved() || attempt.isNameSurrendered()) {
            return new AttemptResult(attempt.isNameSolved(), stateFor(date));
        }
        if (attempt.getNameAttemptsUsed() >= MAX_ATTEMPTS) {
            return new AttemptResult(false, stateFor(date));
        }

        attempt.setNameAttemptsUsed(attempt.getNameAttemptsUsed() + 1);

        var matcher = new PokemonNameMatcher(catalog.all());
        var matched = matcher.match(transcript);
        boolean correct = matched
            .map(m -> m.equalsIgnoreCase(pokemon.getPokemonName()))
            .orElse(false);

        if (correct) {
            attempt.setNameSolved(true);
            attempt.setNameScore(MAX_ATTEMPTS - (attempt.getNameAttemptsUsed() - 1));
        } else {
            String guess = matched.orElseGet(() -> TextNormalizer.normalize(transcript));
            if (!guess.isEmpty()) {
                attempt.getNameAttempts().add(guess);
            }
            if (attempt.getNameAttemptsUsed() >= MAX_ATTEMPTS) {
                attempt.setNameSurrendered(true);
                attempt.setNameScore(0);
            }
        }

        attemptRepo.save(attempt);
        return new AttemptResult(correct, stateFor(date));
    }

    @Transactional
    public GameState surrender() {
        var date = LocalDate.now();
        var attempt = attemptRepo.findByUserIdAndDate(currentUser.userId(), date)
            .orElseGet(() -> new UserDailyAttempt(currentUser.userId(), date));
        if (attempt.isNameSolved() || attempt.isNameSurrendered()) return stateFor(date);
        attempt.setNameSurrendered(true);
        attempt.setNameScore(0);
        attemptRepo.save(attempt);
        return stateFor(date);
    }

    private GameState stateFor(LocalDate date) {
        var pokemon = pokemonRepo.findById(date).orElseThrow();
        var attempt = attemptRepo.findByUserIdAndDate(currentUser.userId(), date)
            .orElseGet(() -> new UserDailyAttempt(currentUser.userId(), date));
        DailyQuiz quiz = quizRepo.findById(date).orElseThrow();

        int attemptsLeft = Math.max(0, MAX_ATTEMPTS - attempt.getNameAttemptsUsed());
        int blurLevel = Math.min(attempt.getNameAttemptsUsed(), 4);
        boolean revealed = attempt.isNameSolved()
            || attempt.isNameSurrendered()
            || attempt.getNameAttemptsUsed() >= MAX_ATTEMPTS;

        String answer = TextNormalizer.normalize(pokemon.getPokemonName());
        List<NameAttemptView> attemptViews = attempt.getNameAttempts().stream()
            .map(g -> new NameAttemptView(g, WordleEvaluator.evaluate(g, answer)))
            .toList();

        String hints = calculateHints(answer, attemptViews, attempt.getNameAttemptsUsed());

        return new GameState(
            attemptsLeft,
            blurLevel,
            attempt.isNameSolved(),
            attempt.isNameSurrendered(),
            revealed ? pokemon.getPokemonName() : null,
            revealed ? pokemon.getPokemonNameEs() : null,
            pokemon.getImageUrl(),
            attempt.getNameScore(),
            quiz.getStatus() == QuizStatus.READY,
            quiz.getStatus().name(),
            attemptViews,
            answer.length(),
            hints
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

        // Set of characters that the user already knows are in the pokemon name
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
        // First pass: try to add hints that are NOT among known letters
        for (int pos : availablePositions) {
            if (hintsAdded >= 3) break;
            if (knownLetters.size() + hintsAdded >= n - 2) break;

            char c = answer.charAt(pos);
            if (knownLetters.contains(c)) continue;

            hints[pos] = c;
            hintsAdded++;
        }

        // Second pass: if we still have room, add hints from known letters (but still in unknown positions)
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
