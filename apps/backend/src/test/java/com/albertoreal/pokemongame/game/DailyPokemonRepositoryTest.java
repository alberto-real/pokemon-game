package com.albertoreal.pokemongame.game;

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
class DailyPokemonRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pokemon_game")
        .withUsername("pokemon")
        .withPassword("pokemon_dev_password");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired DailyPokemonRepository repository;

    @Test
    void persistsAndRetrievesByDate() {
        var today = LocalDate.of(2026, 4, 22);
        var pokemon = new DailyPokemon(today, 25, "pikachu", "Pikachu",
            "http://img/pikachu.png", OffsetDateTime.now());

        repository.save(pokemon);
        var found = repository.findByDate(today);

        assertThat(found).isPresent();
        assertThat(found.get().getPokemonName()).isEqualTo("pikachu");
        assertThat(found.get().getPokemonNameEs()).isEqualTo("Pikachu");
    }
}
