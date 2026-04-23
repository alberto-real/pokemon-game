package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@ConditionalOnProperty(prefix = "pokemon-game.ai", name = "provider",
                       havingValue = "stub", matchIfMissing = true)
public class StubQuizGenerator implements QuizGenerator {

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        String name = pokemon.name();
        return List.of(
            new GeneratedQuestion(
                "¿Cuál es el nombre del Pokemon mostrado?",
                List.of(name, "ditto", "eevee", "snorlax"), 0),
            new GeneratedQuestion(
                "¿De qué región proviene originalmente " + name + "?",
                List.of("Kanto", "Johto", "Hoenn", "Sinnoh"), 0),
            new GeneratedQuestion(
                "¿Cuál es el tipo principal de " + name + "?",
                List.of("Normal", "Fuego", "Agua", "Planta"), 0),
            new GeneratedQuestion(
                "¿Cuántas evoluciones tiene " + name + "?",
                List.of("Ninguna", "Una", "Dos", "Tres"), 1),
            new GeneratedQuestion(
                "¿Fue " + name + " introducido en la primera generación?",
                List.of("Sí", "No", "Parcialmente", "Desconocido"), 0)
        );
    }
}
