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
        var species = new PokemonSpeciesDto(
            25,
            List.of(new PokemonSpeciesDto.Name("Pikachu", new PokemonSpeciesDto.Language("es"))),
            new PokemonSpeciesDto.Generation("generation-i"),
            null,
            List.of(new PokemonSpeciesDto.Genus("Pokémon Ratón",
                new PokemonSpeciesDto.Language("es"))));
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
        assertThat(ctx).contains("Pokémon Ratón");
        assertThat(ctx).contains("generation-i");
        assertThat(ctx).contains("Kanto");
        assertThat(ctx).contains("pichu");
        assertThat(ctx).contains("raichu");
        // Height/weight intentionally excluded — too specific for good quiz questions
        assertThat(ctx).doesNotContain("Altura");
        assertThat(ctx).doesNotContain("Peso");
    }

    @Test
    void fallsBackToEnglishNameWhenSpanishMissing() {
        var pokemon = new PokemonDto(132, "ditto", 3, 40, null, List.of(), List.of());
        var species = new PokemonSpeciesDto(
            132,
            List.of(new PokemonSpeciesDto.Name("Ditto", new PokemonSpeciesDto.Language("en"))),
            null,
            null,
            null);

        var ctx = new RagContextBuilder().build(pokemon, species, null);
        assertThat(ctx).contains("ditto");
    }

    @Test
    void handlesNullSpeciesAndChain() {
        var pokemon = new PokemonDto(1, "bulbasaur", 7, 69, null, List.of(), List.of());
        var ctx = new RagContextBuilder().build(pokemon, null, null);
        assertThat(ctx).contains("bulbasaur");
        assertThat(ctx).doesNotContain("Generación");
        assertThat(ctx).doesNotContain("Categoría");
    }

    @Test
    void mapsAllNineGenerationsToRegions() {
        record Pair(String gen, String region) {}
        var pairs = List.of(
            new Pair("generation-i", "Kanto"),
            new Pair("generation-ii", "Johto"),
            new Pair("generation-iii", "Hoenn"),
            new Pair("generation-iv", "Sinnoh"),
            new Pair("generation-v", "Teselia"),
            new Pair("generation-vi", "Kalos"),
            new Pair("generation-vii", "Alola"),
            new Pair("generation-viii", "Galar"),
            new Pair("generation-ix", "Paldea"));

        var pokemon = new PokemonDto(1, "x", 1, 1, null, List.of(), List.of());
        for (Pair p : pairs) {
            var species = new PokemonSpeciesDto(1,
                List.of(new PokemonSpeciesDto.Name("X", new PokemonSpeciesDto.Language("es"))),
                new PokemonSpeciesDto.Generation(p.gen()), null, null);
            var ctx = new RagContextBuilder().build(pokemon, species, null);
            assertThat(ctx).contains(p.region());
        }
    }
}
