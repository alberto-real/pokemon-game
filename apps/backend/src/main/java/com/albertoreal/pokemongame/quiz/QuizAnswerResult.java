package com.albertoreal.pokemongame.quiz;

public record QuizAnswerResult(
    boolean correct,
    int selectedIndex,
    int correctIndex,
    boolean quizComplete,
    Integer quizScore,
    Integer totalScore) {}
