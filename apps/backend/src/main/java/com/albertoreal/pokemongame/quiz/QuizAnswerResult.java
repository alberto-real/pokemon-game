package com.albertoreal.pokemongame.quiz;

public record QuizAnswerResult(
    boolean correct,
    int selectedIndex,
    int correctIndex
) {}
