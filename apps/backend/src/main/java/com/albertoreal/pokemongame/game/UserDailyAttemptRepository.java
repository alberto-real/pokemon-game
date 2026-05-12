package com.albertoreal.pokemongame.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface UserDailyAttemptRepository
        extends JpaRepository<UserDailyAttempt, UserDailyAttempt.Key> {
    Optional<UserDailyAttempt> findByUserIdAndDate(String userId, LocalDate date);
    void deleteByDate(LocalDate date);
}
