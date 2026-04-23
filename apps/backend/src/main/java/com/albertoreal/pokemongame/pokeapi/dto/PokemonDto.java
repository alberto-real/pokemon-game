package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(int id, String name, Sprites sprites) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(Other other) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Other(@JsonProperty("official-artwork") OfficialArtwork officialArtwork) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record OfficialArtwork(@JsonProperty("front_default") String frontDefault) {}
    }

    public String officialArtworkUrl() {
        return sprites != null && sprites.other() != null && sprites.other().officialArtwork() != null
            ? sprites.other().officialArtwork().frontDefault() : null;
    }
}
