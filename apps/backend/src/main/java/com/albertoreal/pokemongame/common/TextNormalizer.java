package com.albertoreal.pokemongame.common;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");

    private TextNormalizer() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String trimmed = input.trim().toLowerCase();
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        String noAccents = DIACRITICS.matcher(decomposed).replaceAll("");
        return NON_ALNUM.matcher(noAccents).replaceAll("");
    }
}
