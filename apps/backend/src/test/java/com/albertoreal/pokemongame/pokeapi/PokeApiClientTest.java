package com.albertoreal.pokemongame.pokeapi;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiClientTest {

    @Test
    void dtoExtractsOfficialArtworkUrl() {
        var pokemon = new PokemonDto(25, "pikachu",
            new PokemonDto.Sprites(
                new PokemonDto.Sprites.Other(
                    new PokemonDto.Sprites.OfficialArtwork("http://img/pikachu.png"))));
        assertThat(pokemon.officialArtworkUrl()).isEqualTo("http://img/pikachu.png");
    }

    @Test
    void dtoReturnsNullWhenSpritesMissing() {
        var pokemon = new PokemonDto(25, "pikachu", null);
        assertThat(pokemon.officialArtworkUrl()).isNull();
    }

    @Test
    void speciesDtoFindsNameInLanguage() {
        var species = new PokemonSpeciesDto(25, List.of(
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("en")),
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))
        ));
        assertThat(species.nameInLanguage("es")).isEqualTo("Pikachu");
        assertThat(species.nameInLanguage("fr")).isNull();
    }

    @Test
    void speciesDtoHandlesNullNames() {
        var species = new PokemonSpeciesDto(25, null);
        assertThat(species.nameInLanguage("es")).isNull();
    }
}
