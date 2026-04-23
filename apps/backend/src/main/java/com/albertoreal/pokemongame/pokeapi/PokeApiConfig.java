package com.albertoreal.pokemongame.pokeapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-game.pokeapi")
public record PokeApiConfig(String baseUrl) {
    public PokeApiConfig {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://pokeapi.co/api/v2";
    }
}
