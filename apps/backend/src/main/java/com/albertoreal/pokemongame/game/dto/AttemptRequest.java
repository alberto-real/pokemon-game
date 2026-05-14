package com.albertoreal.pokemongame.game.dto;

import com.albertoreal.pokemongame.game.NameAttemptView;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * @param transcript        what the user said/typed for this attempt
 * @param previousAttempts  attempts already made in this session, with their
 *                          feedback (echoed back from prior responses).
 *                          Required so the server can compute hints and
 *                          decide whether the game is now over.
 */
public record AttemptRequest(
    @NotBlank String transcript,
    List<NameAttemptView> previousAttempts
) {}
