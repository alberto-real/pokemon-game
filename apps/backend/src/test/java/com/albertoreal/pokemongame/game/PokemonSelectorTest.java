package com.albertoreal.pokemongame.game;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonSelectorTest {

    @Test
    void pickAvoidsUsedIds() {
        var selector = new PokemonSelector(5, new Random(42));
        var used = Set.of(1, 2, 3);
        int pick = selector.pick(used);
        assertThat(pick).isBetween(4, 5);
        assertThat(used).doesNotContain(pick);
    }

    @Test
    void pickReturnsValidIdWhenAllUnused() {
        var selector = new PokemonSelector(10, new Random());
        int pick = selector.pick(Set.of());
        assertThat(pick).isBetween(1, 10);
    }

    @Test
    void throwsWhenAllUsed() {
        var selector = new PokemonSelector(3, new Random());
        assertThatThrownBy(() -> selector.pick(Set.of(1, 2, 3)))
            .isInstanceOf(IllegalStateException.class);
    }
}
