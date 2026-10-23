package com.albertoreal.pokemongame.game;

/**
 * Immutable per-day info served on bootstrap. The frontend keeps the
 * mutable session state (attempts, score, solved status) in memory; a
 * page refresh therefore restarts the game.
 */
public record GameState(
    String imageUrl,
    int nameLength,
    int nameLengthEs,
    boolean quizReady,
    String quizStatus,
    int maxAttempts
) {}
