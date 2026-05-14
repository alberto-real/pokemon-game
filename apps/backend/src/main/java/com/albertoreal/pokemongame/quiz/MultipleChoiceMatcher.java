package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.common.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultipleChoiceMatcher {

    /**
     * Minimum normalized similarity (1 - editDistance/maxLen) for a fuzzy
     * fallback match. Tolerates common Spanish ortographic mistakes
     * ("ierva" → "hierba", "fueego" → "fuego") without accepting random
     * unrelated input.
     */
    private static final double FUZZY_THRESHOLD = 0.65;

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

    private final LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();

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

        // 4) Content substring match (transcript contains the option text)
        for (int i = 0; i < options.size(); i++) {
            String optNorm = TextNormalizer.normalize(options.get(i));
            if (!optNorm.isEmpty() && norm.contains(optNorm)) {
                return Optional.of(i);
            }
        }

        // 5) Fuzzy fallback: pick the option with the smallest edit distance,
        //    provided the similarity is above FUZZY_THRESHOLD.
        int bestIdx = -1;
        double bestRatio = 0.0;
        for (int i = 0; i < options.size(); i++) {
            String optNorm = TextNormalizer.normalize(options.get(i));
            if (optNorm.isEmpty()) continue;
            int d = distance.apply(norm, optNorm);
            int maxLen = Math.max(norm.length(), optNorm.length());
            double ratio = maxLen == 0 ? 0.0 : 1.0 - (double) d / maxLen;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestIdx = i;
            }
        }
        if (bestIdx >= 0 && bestRatio >= FUZZY_THRESHOLD) {
            return Optional.of(bestIdx);
        }

        return Optional.empty();
    }
}
