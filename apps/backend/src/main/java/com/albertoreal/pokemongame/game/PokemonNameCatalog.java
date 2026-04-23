package com.albertoreal.pokemongame.game;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PokemonNameCatalog {

    private static final Set<String> SEED = Set.of(
        "bulbasaur", "ivysaur", "venusaur",
        "charmander", "charmeleon", "charizard",
        "squirtle", "wartortle", "blastoise",
        "pikachu", "raichu", "meowth", "mewtwo",
        "mew", "eevee", "snorlax", "gyarados",
        "dragonite", "gengar", "lucario"
    );

    private final Set<String> names = new CopyOnWriteArraySet<>(SEED);

    public void register(String name) {
        if (name != null && !name.isBlank()) {
            names.add(name.toLowerCase());
        }
    }

    public Set<String> all() {
        return Set.copyOf(names);
    }
}
