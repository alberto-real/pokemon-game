package com.albertoreal.pokemongame.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyQuizQuestionRepository extends JpaRepository<DailyQuizQuestion, Long> {
    List<DailyQuizQuestion> findByQuizDateOrderByPositionAsc(LocalDate quizDate);
}
