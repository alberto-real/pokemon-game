package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.game.dto.AttemptRequest;
import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
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
class GameControllerIT {

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
    void stubPokeApi() {
        when(pokeApi.getPokemon(anyInt())).thenReturn(new PokemonDto(25, "pikachu", null));
        when(pokeApi.getSpecies(anyInt())).thenReturn(
            new PokemonSpeciesDto(25, List.of(
                new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es")))));
        attemptRepo.deleteAll();
    }

    @Test
    void todayBootstrapsTheDayAndReturnsState() {
        var r = rest.getForEntity("/api/game/today", GameState.class);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r.getBody().attemptsLeft()).isEqualTo(5);
        assertThat(r.getBody().nameSolved()).isFalse();
        assertThat(pokemonRepo.findById(LocalDate.now())).isPresent();
    }

    @Test
    void attemptWithCorrectNameSolves() {
        rest.getForEntity("/api/game/today", GameState.class); // bootstrap
        var r = rest.postForEntity("/api/game/today/attempt",
            new AttemptRequest("pikachu"), AttemptResult.class);
        assertThat(r.getBody().correct()).isTrue();
        assertThat(r.getBody().state().nameScore()).isEqualTo(5);
    }

    @Test
    void surrenderReveals() {
        rest.getForEntity("/api/game/today", GameState.class);
        var r = rest.postForEntity("/api/game/today/surrender", null, GameState.class);
        assertThat(r.getBody().nameSurrendered()).isTrue();
        assertThat(r.getBody().revealedName()).isEqualTo("pikachu");
    }
}
