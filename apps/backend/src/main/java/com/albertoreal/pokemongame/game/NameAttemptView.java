package com.albertoreal.pokemongame.game;

/**
 * View of a past name attempt with per-letter Wordle feedback.
 * <p>
 * {@code feedback} is a string the same length as {@code guess} where each char is:
 * 'H' (hit, correct letter and position), 'P' (present, correct letter wrong position),
 * 'M' (miss, letter not in answer).
 */
public record NameAttemptView(String guess, String feedback) {}
