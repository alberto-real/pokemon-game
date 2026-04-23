package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubQuizGeneratorTest {

    @Test
    void returnsFivePokemonSpecificQuestions() {
        var gen = new StubQuizGenerator();
        var pokemon = new PokemonDto(25, "pikachu", null);
        var questions = gen.generate(pokemon, null);

        assertThat(questions).hasSize(5);
        assertThat(questions.get(0).text()).contains("Pokemon");
        assertThat(questions.get(0).options()).contains("pikachu");
        assertThat(questions.get(0).correctIndex()).isEqualTo(0);
    }

    @Test
    void everyQuestionHasFourOptions() {
        var gen = new StubQuizGenerator();
        var pokemon = new PokemonDto(25, "pikachu", null);
        var questions = gen.generate(pokemon, null);
        for (var q : questions) {
            assertThat(q.options()).hasSize(4);
            assertThat(q.correctIndex()).isBetween(0, 3);
        }
    }
}
