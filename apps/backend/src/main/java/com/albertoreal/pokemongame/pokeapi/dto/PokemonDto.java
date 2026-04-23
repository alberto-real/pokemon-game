package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
    int id,
    String name,
    int height,
    int weight,
    Sprites sprites,
    List<TypeSlot> types,
    List<AbilitySlot> abilities
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(Other other) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Other(@JsonProperty("official-artwork") OfficialArtwork officialArtwork) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record OfficialArtwork(@JsonProperty("front_default") String frontDefault) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeSlot(int slot, Type type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Type(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(Ability ability, @JsonProperty("is_hidden") boolean isHidden) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ability(String name) {}

    public String officialArtworkUrl() {
        return sprites != null && sprites.other() != null
                && sprites.other().officialArtwork() != null
            ? sprites.other().officialArtwork().frontDefault() : null;
    }

    public double heightMeters() { return height / 10.0; }
    public double weightKilograms() { return weight / 10.0; }

    public List<String> typeNames() {
        return types == null ? List.of()
            : types.stream()
                .filter(t -> t.type() != null)
                .map(t -> t.type().name())
                .toList();
    }

    public List<String> abilityNames() {
        return abilities == null ? List.of()
            : abilities.stream()
                .filter(a -> a.ability() != null)
                .map(a -> a.ability().name())
                .toList();
    }
}
