package com.albertoreal.pokemongame.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, LocalDate> {
}
