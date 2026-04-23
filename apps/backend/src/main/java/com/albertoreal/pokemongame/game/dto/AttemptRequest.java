package com.albertoreal.pokemongame.game.dto;

import jakarta.validation.constraints.NotBlank;

public record AttemptRequest(@NotBlank String transcript) {}
