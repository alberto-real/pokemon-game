package com.albertoreal.pokemongame.game;

import java.util.List;

public record GameState(
    int attemptsLeft,
    int blurLevel,          // 0..4 (0 = full shadow; 4 = clear image)
    boolean nameSolved,
    boolean nameSurrendered,
    String revealedName,    // null if not yet revealed
    String revealedNameEs,
    String imageUrl,
    Integer nameScore,      // null while in progress
    boolean quizReady,
    String quizStatus,
    List<NameAttemptView> nameAttempts,
    int nameLength,
    String hints
) {}
