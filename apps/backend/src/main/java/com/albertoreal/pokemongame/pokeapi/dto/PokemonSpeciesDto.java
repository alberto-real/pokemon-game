package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesDto(int id, List<Name> names) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Name(String name, Language language) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Language(String name) {}

    public String nameInLanguage(String lang) {
        if (names == null) return null;
        return names.stream()
            .filter(n -> n.language() != null && lang.equals(n.language().name()))
            .map(Name::name)
            .findFirst()
            .orElse(null);
    }
}
