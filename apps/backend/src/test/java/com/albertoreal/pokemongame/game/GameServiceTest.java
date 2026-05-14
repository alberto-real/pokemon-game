package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.quiz.DailyQuiz;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class GameServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pokemon_game").withUsername("pokemon").withPassword("pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired GameService gameService;
    @Autowired DailyPokemonRepository pokemonRepo;
    @Autowired DailyQuizRepository quizRepo;
    @Autowired PokemonNameCatalog catalog;

    @BeforeEach
    void cleanDb() {
        quizRepo.deleteAll();
        pokemonRepo.deleteAll();
    }

    private void seedPokemon(String name) {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, name, "Pokemon", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        catalog.register(name);
    }

    /**
     * Drives the service like a frontend would: keeps the running attempt list
     * and feeds it back on each call.
     */
    private AttemptResult guess(String transcript, List<NameAttemptView> running) {
        var r = gameService.attempt(transcript, running);
        if (!r.correct() && r.guess() != null && !r.guess().isEmpty()) {
            running.add(new NameAttemptView(r.guess(), r.feedback()));
        }
        return r;
    }

    @Test
    void scoresFiveWhenSolvedOnFirstAttempt() {
        seedPokemon("pikachu");
        var r = gameService.attempt("pikachu", List.of());
        assertThat(r.correct()).isTrue();
        assertThat(r.nameScore()).isEqualTo(5);
        assertThat(r.revealedName()).isEqualTo("pikachu");
    }

    @Test
    void scoresOneWhenSolvedOnFifthAttempt() {
        seedPokemon("charizard");
        var running = new ArrayList<NameAttemptView>();
        for (int i = 1; i <= 4; i++) guess("wrong" + i, running);
        var r = gameService.attempt("Charizard", running);
        assertThat(r.correct()).isTrue();
        assertThat(r.nameScore()).isEqualTo(1);
    }

    @Test
    void scoresZeroWhenAllAttemptsExhausted() {
        seedPokemon("mewtwo");
        var running = new ArrayList<NameAttemptView>();
        AttemptResult last = null;
        for (int i = 1; i <= 5; i++) {
            last = guess("notthepokemon", running);
        }
        assertThat(last).isNotNull();
        assertThat(last.correct()).isFalse();
        assertThat(last.nameScore()).isZero();
        assertThat(last.revealedName()).isEqualTo("mewtwo");
    }

    @Test
    void surrenderRevealsName() {
        seedPokemon("mewtwo");
        var s = gameService.surrender();
        assertThat(s.revealedName()).isEqualTo("mewtwo");
    }

    @Test
    void furtherAttemptsAfterMaxAreIdempotent() {
        seedPokemon("mewtwo");
        var running = new ArrayList<NameAttemptView>();
        for (int i = 1; i <= 5; i++) guess("wrong" + i, running);
        // 5 attempts already in running list — defensive call should still
        // report game over without crashing.
        var r = gameService.attempt("anything", running);
        assertThat(r.revealedName()).isEqualTo("mewtwo");
    }

    @Test
    void todayReturnsImmutableDayInfo() {
        seedPokemon("bulbasaur");
        var s = gameService.today();
        assertThat(s.imageUrl()).isEqualTo("url");
        assertThat(s.nameLength()).isEqualTo(9);
        assertThat(s.quizReady()).isTrue();
        assertThat(s.maxAttempts()).isEqualTo(GameService.MAX_ATTEMPTS);
    }

    @Test
    void hintsAppearFromThirdAttempt() {
        seedPokemon("bulbasaur"); // 9 letters
        var running = new ArrayList<NameAttemptView>();

        var r1 = guess("ivysaur", running);
        assertThat(r1.hints()).isNull();

        var r2 = guess("venusaur", running);
        assertThat(r2.hints()).isNull();

        // 3rd attempt -> hints
        var r3 = guess("charmander", running);
        assertThat(r3.hints()).isNotNull();
        assertThat(r3.hints()).hasSize(9);
        long hintCount = r3.hints().chars().filter(c -> c != '_').count();
        assertThat(hintCount).isBetween(1L, 3L);

        // Hints leave at least 2 empty spaces
        long emptyCount = r3.hints().chars().filter(c -> c == '_').count();
        assertThat(emptyCount).isGreaterThanOrEqualTo(2L);
    }
}
