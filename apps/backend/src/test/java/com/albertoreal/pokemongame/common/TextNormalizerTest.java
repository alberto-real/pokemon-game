package com.albertoreal.pokemongame.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void lowercasesTrimsAndStripsAccents() {
        assertThat(TextNormalizer.normalize("  Pikachú  ")).isEqualTo("pikachu");
        assertThat(TextNormalizer.normalize("Mewtwo")).isEqualTo("mewtwo");
        assertThat(TextNormalizer.normalize("¡Charizard!")).isEqualTo("charizard");
        assertThat(TextNormalizer.normalize("")).isEqualTo("");
    }

    @Test
    void removesPunctuation() {
        assertThat(TextNormalizer.normalize("Nidoran-♀")).isEqualTo("nidoran");
    }

    @Test
    void handlesNull() {
        assertThat(TextNormalizer.normalize(null)).isEqualTo("");
    }
}
