package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.game.DailyPokemonRepository;
import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@Testcontainers
class QuizOrchestratorIT {

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

    @Autowired QuizOrchestrator orchestrator;
    @Autowired DailyPokemonRepository pokemonRepo;
    @Autowired DailyQuizRepository quizRepo;
    @Autowired DailyQuizQuestionRepository questionRepo;

    @Test
    void generatesDailyQuizIdempotently() {
        when(pokeApi.getPokemon(anyInt())).thenReturn(new PokemonDto(25, "pikachu", null));
        when(pokeApi.getSpecies(anyInt())).thenReturn(
            new PokemonSpeciesDto(25, List.of(
                new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es")))));

        var date = LocalDate.of(2026, 4, 22);
        orchestrator.ensureExists(date);
        // Second invocation must not duplicate, must not fail
        orchestrator.ensureExists(date);

        assertThat(pokemonRepo.findById(date)).isPresent();
        var quiz = quizRepo.findById(date).orElseThrow();
        assertThat(quiz.getStatus()).isEqualTo(QuizStatus.READY);
        assertThat(questionRepo.findByQuizDateOrderByPositionAsc(date)).hasSize(5);
    }
}
