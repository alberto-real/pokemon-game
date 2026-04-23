package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RagContextBuilder {

    public String build(PokemonDto pokemon, PokemonSpeciesDto species, EvolutionChainDto chain) {
        String nameEs = species != null ? species.nameInLanguage("es") : null;
        if (nameEs == null) nameEs = pokemon.name();

        StringBuilder sb = new StringBuilder();
        sb.append("Nombre en castellano: ").append(nameEs).append('\n');
        sb.append("Nombre en inglés: ").append(pokemon.name()).append('\n');
        sb.append("ID PokeAPI: ").append(pokemon.id()).append('\n');
        sb.append(String.format(Locale.US, "Altura: %.1f m%n", pokemon.heightMeters()));
        sb.append(String.format(Locale.US, "Peso: %.1f kg%n", pokemon.weightKilograms()));
        if (!pokemon.typeNames().isEmpty()) {
            sb.append("Tipos: ").append(String.join(", ", pokemon.typeNames())).append('\n');
        }
        if (!pokemon.abilityNames().isEmpty()) {
            sb.append("Habilidades: ").append(String.join(", ", pokemon.abilityNames())).append('\n');
        }
        if (species != null && species.generationName() != null) {
            sb.append("Generación: ").append(species.generationName()).append('\n');
        }
        if (chain != null) {
            sb.append("Cadena evolutiva (en orden): ")
              .append(String.join(" → ", chain.evolutionLine())).append('\n');
        }
        return sb.toString();
    }
}
