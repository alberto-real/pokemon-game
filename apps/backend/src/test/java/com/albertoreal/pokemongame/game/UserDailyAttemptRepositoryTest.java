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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class UserDailyAttemptRepositoryTest {

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

    @Autowired UserDailyAttemptRepository repo;

    @Test
    void storesAttemptWithCompositeKey() {
        var attempt = new UserDailyAttempt("dev", LocalDate.of(2026, 4, 22));
        attempt.setNameAttemptsUsed(2);
        attempt.setNameSolved(true);
        attempt.setNameScore(4);
        repo.save(attempt);

        var found = repo.findByUserIdAndDate("dev", LocalDate.of(2026, 4, 22));
        assertThat(found).isPresent();
        assertThat(found.get().getNameScore()).isEqualTo(4);
        assertThat(found.get().isNameSolved()).isTrue();
    }
}
