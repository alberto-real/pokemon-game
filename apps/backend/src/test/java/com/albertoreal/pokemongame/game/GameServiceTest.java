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
    @Autowired UserDailyAttemptRepository attemptRepo;
    @Autowired PokemonNameCatalog catalog;

    @BeforeEach
    void cleanDb() {
        attemptRepo.deleteAll();
        quizRepo.deleteAll();
        pokemonRepo.deleteAll();
    }

    private void seedPokemon(String name) {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, name, "Pokemon", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        catalog.register(name);
    }

    @Test
    void scoresFiveWhenSolvedOnFirstAttempt() {
        seedPokemon("pikachu");
        var r = gameService.attempt("pikachu");
        assertThat(r.correct()).isTrue();
        assertThat(r.state().nameSolved()).isTrue();
        assertThat(r.state().nameScore()).isEqualTo(5);
        assertThat(r.state().blurLevel()).isEqualTo(1);
    }

    @Test
    void scoresOneWhenSolvedOnFifthAttempt() {
        seedPokemon("charizard");
        for (int i = 1; i <= 4; i++) gameService.attempt("wrong" + i);
        var r = gameService.attempt("Charizard");
        assertThat(r.correct()).isTrue();
        assertThat(r.state().nameScore()).isEqualTo(1);
    }

    @Test
    void scoresZeroWhenAllAttemptsExhausted() {
        seedPokemon("mewtwo");
        for (int i = 1; i <= 5; i++) {
            var r = gameService.attempt("notthepokemon");
            assertThat(r.correct()).isFalse();
        }
        var state = gameService.today();
        assertThat(state.attemptsLeft()).isZero();
        assertThat(state.nameScore()).isZero();
        assertThat(state.revealedName()).isEqualTo("mewtwo");
    }

    @Test
    void surrenderScoresZeroAndReveals() {
        seedPokemon("mewtwo");
        var s = gameService.surrender();
        assertThat(s.nameSurrendered()).isTrue();
        assertThat(s.nameScore()).isZero();
        assertThat(s.revealedName()).isEqualTo("mewtwo");
    }

    @Test
    void furtherAttemptsAfterSolvedAreNoOp() {
        seedPokemon("pikachu");
        gameService.attempt("pikachu");
        var r = gameService.attempt("anything");
        assertThat(r.correct()).isTrue();
        assertThat(r.state().nameScore()).isEqualTo(5);
    }

    @Test
    void providesHintsAndLength() {
        seedPokemon("bulbasaur"); // 9 letters
        var s1 = gameService.today();
        assertThat(s1.nameLength()).isEqualTo(9);
        assertThat(s1.hints()).isNull();

        // 1st failed attempt
        gameService.attempt("ivysaur");
        var s2 = gameService.today();
        assertThat(s2.hints()).isNull();

        // 2nd failed attempt
        gameService.attempt("venusaur");
        var s3 = gameService.today();
        assertThat(s3.hints()).isNull();

        // 3rd failed attempt -> 4th attempt starts
        gameService.attempt("charmander");
        var s4 = gameService.today();
        assertThat(s4.hints()).isNotNull();
        assertThat(s4.hints()).hasSize(9);
        long hintCount = s4.hints().chars().filter(c -> c != '_').count();
        assertThat(hintCount).isBetween(1L, 3L);

        // Check that hints leave at least 2 empty spaces (since we have no greens/oranges yet)
        long emptyCount = s4.hints().chars().filter(c -> c == '_').count();
        assertThat(emptyCount).isGreaterThanOrEqualTo(2L);
    }
}
