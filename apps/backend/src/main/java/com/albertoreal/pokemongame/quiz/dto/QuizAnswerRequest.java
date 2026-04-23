package com.albertoreal.pokemongame.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizAnswerRequest(@NotBlank String transcript) {}
