package com.albertoreal.pokemongame.game;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PokemonNameCatalogTest {

    @Test
    void containsSeedPokemonByDefault() {
        var catalog = new PokemonNameCatalog();
        assertThat(catalog.all()).contains("pikachu", "charizard", "mewtwo", "bulbasaur");
    }

    @Test
    void registersNewNameLowercased() {
        var catalog = new PokemonNameCatalog();
        catalog.register("Mantyke");
        assertThat(catalog.all()).contains("mantyke");
    }

    @Test
    void ignoresBlankAndNull() {
        var catalog = new PokemonNameCatalog();
        int before = catalog.all().size();
        catalog.register(null);
        catalog.register("");
        catalog.register("   ");
        assertThat(catalog.all()).hasSize(before);
    }
}
