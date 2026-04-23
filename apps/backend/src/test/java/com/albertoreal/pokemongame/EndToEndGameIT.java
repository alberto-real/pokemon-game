package com.albertoreal.pokemongame;

import com.albertoreal.pokemongame.game.AttemptResult;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
import com.albertoreal.pokemongame.game.GameState;
import com.albertoreal.pokemongame.game.UserDailyAttemptRepository;
import com.albertoreal.pokemongame.game.dto.AttemptRequest;
import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import com.albertoreal.pokemongame.quiz.QuizAnswerResult;
import com.albertoreal.pokemongame.quiz.QuizView;
import com.albertoreal.pokemongame.quiz.dto.QuizAnswerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class EndToEndGameIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pokemon_game").withUsername("pokemon").withPassword("pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean PokeApiClient pokeApi;

    @Autowired TestRestTemplate rest;
    @Autowired DailyPokemonRepository pokemonRepo;
    @Autowired UserDailyAttemptRepository attemptRepo;

    @BeforeEach
    void seed() {
        when(pokeApi.getPokemon(anyInt())).thenReturn(
            new PokemonDto(25, "pikachu", 4, 60, null, List.of(), List.of()));
        when(pokeApi.getSpecies(anyInt())).thenReturn(
            new PokemonSpeciesDto(25, List.of(
                new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))),
                null, null));
        attemptRepo.deleteAll();
    }

    @Test
    void fullDayFlow_fourMissesThenSolve_thenPerfectQuiz_score6() {
        // 1. Bootstrap the day
        var state = rest.getForEntity("/api/game/today", GameState.class);
        assertThat(state.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(state.getBody().attemptsLeft()).isEqualTo(5);
        assertThat(pokemonRepo.findById(LocalDate.now())).isPresent();

        // 2. Four wrong attempts
        for (int i = 0; i < 4; i++) {
            var r = rest.postForEntity("/api/game/today/attempt",
                new AttemptRequest("zzzzz" + i), AttemptResult.class);
            assertThat(r.getBody().correct()).isFalse();
        }

        // 3. Solve on 5th attempt -> nameScore = 1
        var hit = rest.postForEntity("/api/game/today/attempt",
            new AttemptRequest("pikachu"), AttemptResult.class);
        assertThat(hit.getBody().correct()).isTrue();
        assertThat(hit.getBody().state().nameScore()).isEqualTo(1);

        // 4. Get quiz
        var quizView = rest.getForObject("/api/game/today/quiz", QuizView.class);
        assertThat(quizView.questions()).hasSize(5);

        // 5. Answer each question. StubQuizGenerator correctIndex per question: [0,0,0,1,0].
        // We send letter transcripts ("a"/"b") rather than option text to avoid the
        // MultipleChoiceMatcher ordinal-word rule ("una"/"uno" map to indices) which
        // would mis-match for Q4 where the correct option text "Una" collides with the
        // ordinal word for index 0. Letters are the highest-priority matcher rule.
        int[] correctIndices = {0, 0, 0, 1, 0};
        String[] letters = {"a", "b", "c", "d"};
        for (int i = 0; i < 5; i++) {
            var q = quizView.questions().get(i);
            String letterTranscript = letters[correctIndices[i]];
            var r = rest.postForEntity(
                "/api/game/today/quiz/answer/" + q.id(),
                new QuizAnswerRequest(letterTranscript), QuizAnswerResult.class);
            if (i < 4) {
                assertThat(r.getBody().quizComplete()).isFalse();
                assertThat(r.getBody().correct()).isTrue();
            } else {
                assertThat(r.getBody().quizComplete()).isTrue();
                assertThat(r.getBody().correct()).isTrue();
                assertThat(r.getBody().quizScore()).isEqualTo(5);
                assertThat(r.getBody().totalScore()).isEqualTo(6);
            }
        }

        // 6. Double-check DB state
        var persisted = attemptRepo.findByUserIdAndDate("dev", LocalDate.now()).orElseThrow();
        assertThat(persisted.getNameScore()).isEqualTo(1);
        assertThat(persisted.getQuizScore()).isEqualTo(5);
        assertThat(persisted.getTotalScore()).isEqualTo(6);
        assertThat(persisted.isNameSolved()).isTrue();
        assertThat(persisted.getCompletedAt()).isNotNull();
    }
}
