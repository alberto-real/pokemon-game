package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.common.CurrentUser;
import com.albertoreal.pokemongame.quiz.DailyQuiz;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
        } else if (attempt.getNameAttemptsUsed() >= MAX_ATTEMPTS) {
            attempt.setNameScore(0);
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
            quiz.getStatus().name()
        );
    }
}
