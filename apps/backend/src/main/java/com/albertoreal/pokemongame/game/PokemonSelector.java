package com.albertoreal.pokemongame.game;

import java.util.Random;
import java.util.Set;

public class PokemonSelector {

    private final int maxId;
    private final Random random;

    public PokemonSelector(int maxId, Random random) {
        this.maxId = maxId;
        this.random = random;
    }

    public int pick(Set<Integer> used) {
        if (used.size() >= maxId) {
            throw new IllegalStateException("All Pokemon IDs have been used");
        }
        int candidate;
        do {
            candidate = random.nextInt(maxId) + 1;
        } while (used.contains(candidate));
        return candidate;
    }
}
