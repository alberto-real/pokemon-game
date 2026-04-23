package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.game.DailyPokemon;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DailyQuizRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pokemon_game")
        .withUsername("pokemon")
        .withPassword("pokemon_dev_password");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> {
            String url = postgres.getJdbcUrl();
            return url + (url.contains("?") ? "&" : "?") + "stringtype=unspecified";
        });
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired DailyPokemonRepository pokemonRepo;
    @Autowired DailyQuizRepository quizRepo;
    @Autowired DailyQuizQuestionRepository questionRepo;

    @Test
    void persistsQuizWithQuestions() {
        var date = LocalDate.of(2026, 4, 22);
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu",
            "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));

        var q = new DailyQuizQuestion(date, 1, "¿Tipo?",
            List.of("Fuego", "Electric", "Agua", "Planta"), 1);
        questionRepo.save(q);

        var found = questionRepo.findByQuizDateOrderByPositionAsc(date);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOptions())
            .containsExactly("Fuego", "Electric", "Agua", "Planta");
        assertThat(found.get(0).getCorrectOptionIndex()).isEqualTo(1);
    }
}
