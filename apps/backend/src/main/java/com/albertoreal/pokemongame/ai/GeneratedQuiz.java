package com.albertoreal.pokemongame.ai;

import java.util.List;

/** Wrapper used as the structured-output target for LLM quiz generation. */
public record GeneratedQuiz(List<GeneratedQuestion> questions) {}
