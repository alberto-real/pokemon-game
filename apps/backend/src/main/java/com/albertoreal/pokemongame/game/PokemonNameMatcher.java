package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.common.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PokemonNameMatcher {

    private static final double THRESHOLD = 0.75;
    private final Set<String> normalizedCatalog;
    private final LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();

    public PokemonNameMatcher(Set<String> catalog) {
        this.normalizedCatalog = catalog.stream()
            .map(TextNormalizer::normalize)
            .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<String> match(String input) {
        String norm = TextNormalizer.normalize(input);
        if (norm.isEmpty()) return Optional.empty();

        String best = null;
        double bestRatio = 0.0;
        for (String candidate : normalizedCatalog) {
            int d = distance.apply(norm, candidate);
            int maxLen = Math.max(norm.length(), candidate.length());
            double ratio = maxLen == 0 ? 0 : 1.0 - (double) d / maxLen;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = candidate;
            }
        }
        return bestRatio >= THRESHOLD ? Optional.of(best) : Optional.empty();
    }
}
