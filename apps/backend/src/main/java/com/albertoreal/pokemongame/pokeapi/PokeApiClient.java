package com.albertoreal.pokemongame.pokeapi;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PokeApiClient {

    private final RestClient client;
    private final ConcurrentMap<Integer, PokemonDto> pokemonCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, PokemonSpeciesDto> speciesCache = new ConcurrentHashMap<>();

    public PokeApiClient(PokeApiConfig config) {
        this.client = RestClient.builder().baseUrl(config.baseUrl()).build();
    }

    public PokemonDto getPokemon(int id) {
        return pokemonCache.computeIfAbsent(id, i ->
            client.get().uri("/pokemon/{id}", i).retrieve().body(PokemonDto.class));
    }

    public PokemonSpeciesDto getSpecies(int id) {
        return speciesCache.computeIfAbsent(id, i ->
            client.get().uri("/pokemon-species/{id}", i).retrieve().body(PokemonSpeciesDto.class));
    }
}
