package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.game.DailyPokemon;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class QuizServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pokemon_game").withUsername("pokemon").withPassword("pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired QuizService quizService;
    @Autowired DailyPokemonRepository pokemonRepo;
    @Autowired DailyQuizRepository quizRepo;
    @Autowired DailyQuizQuestionRepository questionRepo;

    @BeforeEach
    void clean() {
        questionRepo.deleteAll();
        quizRepo.deleteAll();
        pokemonRepo.deleteAll();
    }

    @Test
    void todayQuizReturnsAllQuestions() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        for (int i = 1; i <= 5; i++) {
            questionRepo.save(new DailyQuizQuestion(date, i, "q" + i,
                List.of("A","B","C","D"), 0));
        }

        var view = quizService.todayQuiz();
        assertThat(view.questions()).hasSize(5);
        assertThat(view.questions().get(0).position()).isEqualTo(1);
        assertThat(view.questions().get(4).position()).isEqualTo(5);
    }

    @Test
    void answerByIndexEvaluatesCorrectness() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        questionRepo.save(new DailyQuizQuestion(date, 1, "q1", List.of("A","B","C","D"), 2));
        var question = questionRepo.findByQuizDateOrderByPositionAsc(date).get(0);

        var hit = quizService.answerByIndex(question.getId(), 2);
        assertThat(hit.correct()).isTrue();
        assertThat(hit.selectedIndex()).isEqualTo(2);
        assertThat(hit.correctIndex()).isEqualTo(2);

        var miss = quizService.answerByIndex(question.getId(), 0);
        assertThat(miss.correct()).isFalse();
        assertThat(miss.selectedIndex()).isZero();
        assertThat(miss.correctIndex()).isEqualTo(2);
    }

    @Test
    void answerByTranscriptResolvesViaMatcher() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        questionRepo.save(new DailyQuizQuestion(date, 1, "q1",
            List.of("Alpha","Bravo","Charlie","Delta"), 1));
        var question = questionRepo.findByQuizDateOrderByPositionAsc(date).get(0);

        // "B" should match index 1 via the letter rule.
        var r = quizService.answerByTranscript(question.getId(), "b");
        assertThat(r.correct()).isTrue();
        assertThat(r.selectedIndex()).isEqualTo(1);
    }
}
