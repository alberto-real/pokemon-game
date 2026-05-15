package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.quiz.DailyQuiz;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GameService that exercise pure logic — no Spring, no DB.
 * For DB-backed integration scenarios see {@link GameServiceTest}.
 */
class GameServiceUnitTest {

    private DailyPokemonRepository pokemonRepo;
    private DailyQuizRepository quizRepo;
    private PokemonNameCatalog catalog;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        pokemonRepo = mock(DailyPokemonRepository.class);
        quizRepo = mock(DailyQuizRepository.class);
        catalog = new PokemonNameCatalog();
        gameService = new GameService(pokemonRepo, quizRepo, catalog);
    }

    private void seedPokemonWithoutRegistering(String name) {
        var date = LocalDate.now();
        var pokemon = new DailyPokemon(date, 25, name, "Pokemon", "url", OffsetDateTime.now());
        when(pokemonRepo.findById(any(LocalDate.class))).thenReturn(Optional.of(pokemon));
        when(quizRepo.findById(any(LocalDate.class)))
            .thenReturn(Optional.of(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now())));
    }

    @Test
    void exactGuessIsCorrectEvenWhenCatalogDoesNotKnowName() {
        // Repro of yesterday's bug: backend restart wipes the in-memory
        // catalog, the day's quiz already exists in DB so QuizOrchestrator
        // does not re-register the name. An exact guess must still validate.
        seedPokemonWithoutRegistering("krookodile");
        assertThat(catalog.all()).doesNotContain("krookodile");

        var r = gameService.attempt("krookodile", List.of());

        assertThat(r.correct()).isTrue();
        assertThat(r.revealedName()).isEqualTo("krookodile");
        assertThat(r.feedback()).isEqualTo("HHHHHHHHHH");
    }

    @Test
    void todayRegistersPokemonInCatalog() {
        // Defensive: the catalog must be repopulated on bootstrap so voice
        // fuzzy matching keeps working after a backend restart.
        seedPokemonWithoutRegistering("garchomp");
        assertThat(catalog.all()).doesNotContain("garchomp");

        gameService.today();

        assertThat(catalog.all()).contains("garchomp");
    }
}
