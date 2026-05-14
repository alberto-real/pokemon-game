package com.albertoreal.pokemongame.quiz;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MultipleChoiceMatcherTest {

    static final List<String> OPTIONS = List.of("Kanto", "Johto", "Hoenn", "Sinnoh");
    static final MultipleChoiceMatcher m = new MultipleChoiceMatcher();

    @Test
    void matchesByLetterExact() {
        assertThat(m.match("A", OPTIONS)).hasValue(0);
        assertThat(m.match("b", OPTIONS)).hasValue(1);
        assertThat(m.match("C", OPTIONS)).hasValue(2);
        assertThat(m.match("D", OPTIONS)).hasValue(3);
    }

    @Test
    void matchesByLetterWithPrefix() {
        assertThat(m.match("la b", OPTIONS)).hasValue(1);
        assertThat(m.match("letra c", OPTIONS)).hasValue(2);
        assertThat(m.match("el A", OPTIONS)).hasValue(0);
    }

    @Test
    void matchesByOrdinal() {
        assertThat(m.match("uno", OPTIONS)).hasValue(0);
        assertThat(m.match("la dos", OPTIONS)).hasValue(1);
        assertThat(m.match("cuatro", OPTIONS)).hasValue(3);
        assertThat(m.match("la primera", OPTIONS)).hasValue(0);
        assertThat(m.match("la tercera", OPTIONS)).hasValue(2);
    }

    @Test
    void matchesByLast() {
        assertThat(m.match("la última", OPTIONS)).hasValue(3);
        assertThat(m.match("último", OPTIONS)).hasValue(3);
    }

    @Test
    void matchesByContent() {
        assertThat(m.match("Kanto", OPTIONS)).hasValue(0);
        assertThat(m.match("creo que Hoenn", OPTIONS)).hasValue(2);
        assertThat(m.match("Sinnoh", OPTIONS)).hasValue(3);
    }

    @Test
    void rejectsUnknown() {
        assertThat(m.match("amarillo", OPTIONS)).isEmpty();
        assertThat(m.match("", OPTIONS)).isEmpty();
        assertThat(m.match(null, OPTIONS)).isEmpty();
    }

    @Test
    void matchesByFuzzyTypo() {
        var types = List.of("Fuego", "Agua", "Hierba", "Venenoso");
        // Common Spanish ortographic mistakes
        assertThat(m.match("ierva", types)).hasValue(2);   // hierba
        assertThat(m.match("fueego", types)).hasValue(0);  // fuego
        assertThat(m.match("benenoso", types)).hasValue(3); // venenoso
    }

    @Test
    void fuzzyDoesNotOvermatchUnrelatedInput() {
        var types = List.of("Fuego", "Agua", "Hierba", "Venenoso");
        // Far enough from every option, even in edit distance terms
        assertThat(m.match("electrico", types)).isEmpty();
        assertThat(m.match("psiquico", types)).isEmpty();
    }
}
