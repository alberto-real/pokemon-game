package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesDto(
    int id,
    List<Name> names,
    Generation generation,
    @JsonProperty("evolution_chain") EvolutionChainRef evolutionChain
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Name(String name, Language language) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Language(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Generation(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvolutionChainRef(String url) {}

    public String nameInLanguage(String lang) {
        if (names == null) return null;
        return names.stream()
            .filter(n -> n.language() != null && lang.equals(n.language().name()))
            .map(Name::name)
            .findFirst()
            .orElse(null);
    }

    public String generationName() {
        return generation == null ? null : generation.name();
    }

    public String evolutionChainUrl() {
        return evolutionChain == null ? null : evolutionChain.url();
    }
}
