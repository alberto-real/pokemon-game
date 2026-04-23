package com.albertoreal.pokemongame.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainDto(int id, ChainLink chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(Species species, List<ChainLink> evolves_to) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Species(String name) {}

    public List<String> evolutionLine() {
        var result = new ArrayList<String>();
        if (chain != null) collect(chain, result);
        return result;
    }

    private static void collect(ChainLink link, List<String> out) {
        if (link.species() != null) out.add(link.species().name());
        if (link.evolves_to() != null) {
            for (ChainLink next : link.evolves_to()) collect(next, out);
        }
    }
}
