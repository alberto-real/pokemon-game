package com.albertoreal.pokemongame.quiz;

import java.util.List;

public record QuizView(String status, List<QuestionView> questions) {

    public record QuestionView(Long id, int position, String text, List<String> options) {}
}
