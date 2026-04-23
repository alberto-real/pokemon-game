package com.albertoreal.pokemongame.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-game.storage")
public record MediaConfig(String audioDir) {
    public MediaConfig {
        if (audioDir == null || audioDir.isBlank()) audioDir = "./storage/audio";
    }
}
