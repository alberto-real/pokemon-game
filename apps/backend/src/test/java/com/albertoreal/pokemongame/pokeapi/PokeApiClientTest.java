package com.albertoreal.pokemongame.pokeapi;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiClientTest {

    @Test
    void dtoExtractsOfficialArtworkUrl() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60,
            new PokemonDto.Sprites(
                new PokemonDto.Sprites.Other(
                    new PokemonDto.Sprites.OfficialArtwork("http://img/pikachu.png"))),
            List.of(), List.of());
        assertThat(pokemon.officialArtworkUrl()).isEqualTo("http://img/pikachu.png");
    }

    @Test
    void dtoReturnsNullWhenSpritesMissing() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60, null, List.of(), List.of());
        assertThat(pokemon.officialArtworkUrl()).isNull();
    }

    @Test
    void speciesDtoFindsNameInLanguage() {
        var species = new PokemonSpeciesDto(25, List.of(
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("en")),
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))
        ), null, null, null);
        assertThat(species.nameInLanguage("es")).isEqualTo("Pikachu");
        assertThat(species.nameInLanguage("fr")).isNull();
    }

    @Test
    void speciesDtoHandlesNullNames() {
        var species = new PokemonSpeciesDto(25, null, null, null, null);
        assertThat(species.nameInLanguage("es")).isNull();
    }

    @Test
    void extractsHeightAndWeightInMeters() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60, null, List.of(), List.of());
        assertThat(pokemon.heightMeters()).isEqualTo(0.4);
        assertThat(pokemon.weightKilograms()).isEqualTo(6.0);
    }

    @Test
    void extractsTypeAndAbilityNames() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60, null,
            List.of(new PokemonDto.TypeSlot(1, new PokemonDto.Type("electric"))),
            List.of(new PokemonDto.AbilitySlot(new PokemonDto.Ability("static"), false)));
        assertThat(pokemon.typeNames()).containsExactly("electric");
        assertThat(pokemon.abilityNames()).containsExactly("static");
    }

    @Test
    void evolutionChainExtractsLine() {
        var chain = new EvolutionChainDto(1, new EvolutionChainDto.ChainLink(
            new EvolutionChainDto.Species("pichu"),
            List.of(new EvolutionChainDto.ChainLink(
                new EvolutionChainDto.Species("pikachu"),
                List.of(new EvolutionChainDto.ChainLink(
                    new EvolutionChainDto.Species("raichu"),
                    List.of()))))));
        assertThat(chain.evolutionLine()).containsExactly("pichu", "pikachu", "raichu");
    }
}
