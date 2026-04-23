package com.albertoreal.pokemongame.game;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class GameConfig {

    private static final int MAX_POKEMON_ID = 1025;

    @Bean
    public PokemonSelector pokemonSelector() {
        return new PokemonSelector(MAX_POKEMON_ID, new Random());
    }
}
