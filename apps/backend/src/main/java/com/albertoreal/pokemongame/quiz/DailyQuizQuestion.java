package com.albertoreal.pokemongame.quiz;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "daily_quiz_question")
public class DailyQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @Column(nullable = false)
    private int position;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    protected DailyQuizQuestion() {}

    public DailyQuizQuestion(LocalDate quizDate, int position, String questionText,
                             List<String> options, int correctOptionIndex) {
        this.quizDate = quizDate;
        this.position = position;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public Long getId() { return id; }
    public LocalDate getQuizDate() { return quizDate; }
    public int getPosition() { return position; }
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
}
