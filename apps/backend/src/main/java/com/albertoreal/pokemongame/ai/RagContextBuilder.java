package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RagContextBuilder {

    private static final Map<String, String> GENERATION_TO_REGION = Map.ofEntries(
        Map.entry("generation-i", "Kanto"),
        Map.entry("generation-ii", "Johto"),
        Map.entry("generation-iii", "Hoenn"),
        Map.entry("generation-iv", "Sinnoh"),
        Map.entry("generation-v", "Teselia"),
        Map.entry("generation-vi", "Kalos"),
        Map.entry("generation-vii", "Alola"),
        Map.entry("generation-viii", "Galar"),
        Map.entry("generation-ix", "Paldea")
    );

    public String build(PokemonDto pokemon, PokemonSpeciesDto species, EvolutionChainDto chain) {
        String nameEs = species != null ? species.nameInLanguage("es") : null;
        if (nameEs == null) nameEs = pokemon.name();

        StringBuilder sb = new StringBuilder();
        sb.append("Nombre en castellano: ").append(nameEs).append('\n');
        sb.append("Nombre en inglés: ").append(pokemon.name()).append('\n');
        sb.append("ID PokeAPI: ").append(pokemon.id()).append('\n');

        String genus = species == null ? null : species.genusInLanguage("es");
        if (genus == null && species != null) genus = species.genusInLanguage("en");
        if (genus != null) {
            sb.append("Categoría: ").append(genus).append('\n');
        }

        if (!pokemon.typeNames().isEmpty()) {
            sb.append("Tipos: ").append(String.join(", ", pokemon.typeNames())).append('\n');
        }
        if (!pokemon.abilityNames().isEmpty()) {
            sb.append("Habilidades: ").append(String.join(", ", pokemon.abilityNames())).append('\n');
        }
        if (species != null && species.generationName() != null) {
            String generation = species.generationName();
            sb.append("Generación: ").append(generation).append('\n');
            String region = GENERATION_TO_REGION.get(generation);
            if (region != null) {
                sb.append("Región: ").append(region).append('\n');
            }
        }
        if (chain != null) {
            sb.append("Cadena evolutiva (en orden): ")
              .append(String.join(" → ", chain.evolutionLine())).append('\n');
        }
        return sb.toString();
    }
}
