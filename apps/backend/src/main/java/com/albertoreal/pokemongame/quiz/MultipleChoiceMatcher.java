package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.common.TextNormalizer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultipleChoiceMatcher {

    private static final Map<String, Integer> ORDINALS = Map.ofEntries(
        Map.entry("uno", 0), Map.entry("una", 0),
        Map.entry("primero", 0), Map.entry("primera", 0),
        Map.entry("dos", 1),
        Map.entry("segundo", 1), Map.entry("segunda", 1),
        Map.entry("tres", 2),
        Map.entry("tercero", 2), Map.entry("tercera", 2),
        Map.entry("cuatro", 3),
        Map.entry("cuarto", 3), Map.entry("cuarta", 3)
    );

    private static final Map<String, Integer> LETTERS = Map.of(
        "a", 0, "b", 1, "c", 2, "d", 3
    );

    public Optional<Integer> match(String transcript, List<String> options) {
        if (transcript == null || options == null || options.isEmpty()) {
            return Optional.empty();
        }
        String norm = TextNormalizer.normalize(transcript);
        if (norm.isEmpty()) return Optional.empty();

        // 1) Letter exact or with prefix
        for (var e : LETTERS.entrySet()) {
            String letter = e.getKey();
            if (norm.equals(letter)
                || norm.equals("la" + letter)
                || norm.equals("el" + letter)
                || norm.equals("letra" + letter)) {
                if (e.getValue() < options.size()) return Optional.of(e.getValue());
            }
        }

        // 2) "ultimo/ultima"
        if (norm.equals("ultimo") || norm.equals("ultima")
            || norm.equals("laultima") || norm.equals("elultimo")) {
            return Optional.of(options.size() - 1);
        }

        // 3) Ordinal words (substring match to tolerate "la dos" -> "ladós" -> "lados")
        for (var e : ORDINALS.entrySet()) {
            if (norm.contains(e.getKey())) {
                int idx = e.getValue();
                if (idx < options.size()) return Optional.of(idx);
            }
        }

        // 4) Content substring match
        for (int i = 0; i < options.size(); i++) {
            String optNorm = TextNormalizer.normalize(options.get(i));
            if (!optNorm.isEmpty() && norm.contains(optNorm)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }
}
