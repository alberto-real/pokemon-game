package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;

import java.util.List;

public interface QuizGenerator {
    List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species);
}
