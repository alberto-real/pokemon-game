package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextBuilderTest {

    @Test
    void buildsContextWithAllFacts() {
        var pokemon = new PokemonDto(25, "pikachu", 4, 60, null,
            List.of(new PokemonDto.TypeSlot(1, new PokemonDto.Type("electric"))),
            List.of(new PokemonDto.AbilitySlot(new PokemonDto.Ability("static"), false)));
        var species = new PokemonSpeciesDto(25, List.of(
            new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))),
            new PokemonSpeciesDto.Generation("generation-i"), null);
        var chain = new EvolutionChainDto(1, new EvolutionChainDto.ChainLink(
            new EvolutionChainDto.Species("pichu"),
            List.of(new EvolutionChainDto.ChainLink(
                new EvolutionChainDto.Species("pikachu"),
                List.of(new EvolutionChainDto.ChainLink(
                    new EvolutionChainDto.Species("raichu"),
                    List.of()))))));

        var ctx = new RagContextBuilder().build(pokemon, species, chain);

        assertThat(ctx).contains("Pikachu");
        assertThat(ctx).contains("electric");
        assertThat(ctx).contains("static");
        assertThat(ctx).contains("0.4");
        assertThat(ctx).contains("6.0");
        assertThat(ctx).contains("generation-i");
        assertThat(ctx).contains("pichu");
        assertThat(ctx).contains("raichu");
    }

    @Test
    void fallsBackToEnglishNameWhenSpanishMissing() {
        var pokemon = new PokemonDto(132, "ditto", 3, 40, null, List.of(), List.of());
        var species = new PokemonSpeciesDto(132,
            List.of(new PokemonSpeciesDto.Name("Ditto",
                new PokemonSpeciesDto.Language("en"))), null, null);

        var ctx = new RagContextBuilder().build(pokemon, species, null);
        assertThat(ctx).contains("ditto");
    }

    @Test
    void handlesNullSpeciesAndChain() {
        var pokemon = new PokemonDto(1, "bulbasaur", 7, 69, null, List.of(), List.of());
        var ctx = new RagContextBuilder().build(pokemon, null, null);
        assertThat(ctx).contains("bulbasaur");
        assertThat(ctx).doesNotContain("Generación");
    }
}
