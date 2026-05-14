package com.albertoreal.pokemongame.game;

/**
 * Result of a single name guess. The server only reveals the Pokemon name
 * when the game has ended (correct guess or last attempt used).
 *
 * @param correct       whether the guess matched the Pokemon
 * @param guess         normalized guess (matched against catalog when possible)
 * @param feedback      Wordle-style per-letter feedback for this guess
 * @param hints         partial reveal hint (computed server-side); null while
 *                      first 2 attempts and once the game has ended
 * @param revealedName  Pokemon name (English/canonical); null while in progress
 * @param revealedNameEs Pokemon name (Spanish); null while in progress
 * @param nameScore     final name-phase score; null while in progress
 */
public record AttemptResult(
    boolean correct,
    String guess,
    String feedback,
    String hints,
    String revealedName,
    String revealedNameEs,
    Integer nameScore
) {}
