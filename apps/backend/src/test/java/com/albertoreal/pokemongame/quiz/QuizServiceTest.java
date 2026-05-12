package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.game.DailyPokemon;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
import com.albertoreal.pokemongame.game.UserDailyAttemptRepository;
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
    @Autowired UserDailyAttemptRepository attemptRepo;

    @BeforeEach
    void clean() {
        attemptRepo.deleteAll();
        questionRepo.deleteAll();
        quizRepo.deleteAll();
        pokemonRepo.deleteAll();
    }

    @Test
    void scoresPerfectQuizAndCompletesOnLastAnswer() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        for (int i = 1; i <= 5; i++) {
            questionRepo.save(new DailyQuizQuestion(date, i, "q" + i,
                List.of("A","B","C","D"), 0));
        }

        var questions = questionRepo.findByQuizDateOrderByPositionAsc(date);
        for (int i = 0; i < 5; i++) {
            var r = quizService.answerByTranscript(questions.get(i).getId(), "A");
            if (i < 4) {
                assertThat(r.quizComplete()).isFalse();
            } else {
                assertThat(r.quizComplete()).isTrue();
                assertThat(r.quizScore()).isEqualTo(5);
                assertThat(r.totalScore()).isEqualTo(5); // no name score set
            }
        }
    }

    @Test
    void scoresPartialAndCombinesWithNameScore() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 1, "bulbasaur", "Bulbasaur", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        for (int i = 1; i <= 3; i++) {
            questionRepo.save(new DailyQuizQuestion(date, i, "q" + i,
                List.of("opt1","opt2","opt3","opt4"), i % 4));
        }

        // Pre-seed a name score of 3 for this user
        var attempt = new com.albertoreal.pokemongame.game.UserDailyAttempt("dev", date);
        attempt.setNameSolved(true);
        attempt.setNameAttemptsUsed(3);
        attempt.setNameScore(3);
        attemptRepo.save(attempt);

        var questions = questionRepo.findByQuizDateOrderByPositionAsc(date);
        // Answer correctly the first, wrong the next two (using indices 1, 0, 0 → 1 correct, 2 wrong)
        quizService.answerByIndex(questions.get(0).getId(), 1); // correct
        quizService.answerByIndex(questions.get(1).getId(), 0); // wrong (correct was 2)
        var last = quizService.answerByIndex(questions.get(2).getId(), 0); // wrong (correct was 3)

        assertThat(last.quizComplete()).isTrue();
        assertThat(last.quizScore()).isEqualTo(1);
        assertThat(last.totalScore()).isEqualTo(4); // 3 name + 1 quiz
    }

    @Test
    void filtersOutAlreadyAnsweredQuestions() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        for (int i = 1; i <= 3; i++) {
            questionRepo.save(new DailyQuizQuestion(date, i, "q" + i, List.of("A","B","C","D"), 0));
        }

        var questions = questionRepo.findByQuizDateOrderByPositionAsc(date);
        
        // Answer first question
        quizService.answerByIndex(questions.get(0).getId(), 0);

        // Fetch quiz again
        var view = quizService.todayQuiz();
        assertThat(view.questions()).hasSize(2);
        assertThat(view.questions().get(0).position()).isEqualTo(2);
        assertThat(view.questions().get(1).position()).isEqualTo(3);
    }

    @Test
    void preventsDuplicateAnswersForSameQuestion() {
        var date = LocalDate.now();
        pokemonRepo.save(new DailyPokemon(date, 25, "pikachu", "Pikachu", "url", OffsetDateTime.now()));
        quizRepo.save(new DailyQuiz(date, QuizStatus.READY, OffsetDateTime.now()));
        questionRepo.save(new DailyQuizQuestion(date, 1, "q1", List.of("A","B","C","D"), 0));

        var question = questionRepo.findByQuizDateOrderByPositionAsc(date).get(0);
        
        // First answer
        var r1 = quizService.answerByIndex(question.getId(), 0);
        assertThat(r1.correct()).isTrue();
        assertThat(r1.quizScore()).isEqualTo(1);

        // Second answer for same question
        var r2 = quizService.answerByIndex(question.getId(), 0);
        assertThat(r2.quizScore()).isEqualTo(1); // Should still be 1, not 2
        
        var attempt = attemptRepo.findByUserIdAndDate("dev", date).get();
        assertThat(attempt.getQuizAnswers()).hasSize(1);
    }
}
