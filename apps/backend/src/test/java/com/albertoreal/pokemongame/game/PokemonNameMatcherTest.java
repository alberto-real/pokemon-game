package com.albertoreal.pokemongame.game;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class PokemonNameMatcherTest {

    static final Set<String> CATALOG = Set.of("pikachu", "charizard", "bulbasaur", "mewtwo");

    @Test
    void matchesExactName() {
        var m = new PokemonNameMatcher(CATALOG);
        assertThat(m.match("Pikachu")).hasValue("pikachu");
    }

    @Test
    void matchesWithMinorTypo() {
        var m = new PokemonNameMatcher(CATALOG);
        assertThat(m.match("charizar")).hasValue("charizard");
        assertThat(m.match("pikachú")).hasValue("pikachu");
    }

    @Test
    void rejectsFarName() {
        var m = new PokemonNameMatcher(CATALOG);
        assertThat(m.match("dragonite")).isEmpty();
    }

    @Test
    void rejectsBlank() {
        var m = new PokemonNameMatcher(CATALOG);
        assertThat(m.match("")).isEmpty();
        assertThat(m.match("   ")).isEmpty();
    }
}
