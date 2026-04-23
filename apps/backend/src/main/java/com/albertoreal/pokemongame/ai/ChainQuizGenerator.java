package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
@ConditionalOnProperty(prefix = "pokemon-game.ai", name = "provider", havingValue = "chain")
public class ChainQuizGenerator implements QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(ChainQuizGenerator.class);

    private final List<QuizGenerator> providers;

    public ChainQuizGenerator(OllamaQuizGenerator ollama,
                              ObjectProvider<GroqQuizGenerator> groq,
                              ObjectProvider<OpenRouterQuizGenerator> openRouter) {
        // Order: hosted models first (better quality when available, rate-limited)
        // then local Ollama as always-available fallback.
        this.providers = new ArrayList<>();
        GroqQuizGenerator g = groq.getIfAvailable();
        if (g != null) providers.add(g);
        OpenRouterQuizGenerator or = openRouter.getIfAvailable();
        if (or != null) providers.add(or);
        providers.add(ollama);
        log.info("ChainQuizGenerator initialized with {} providers: {}", providers.size(),
            providers.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        QuizGenerationException last = null;
        for (QuizGenerator provider : providers) {
            try {
                log.info("Trying provider {}", provider.getClass().getSimpleName());
                return provider.generate(pokemon, species);
            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.getClass().getSimpleName(), e.getMessage());
                last = (e instanceof QuizGenerationException qge) ? qge
                    : new QuizGenerationException(e.getMessage(), e);
            }
        }
        throw last != null ? last : new QuizGenerationException("No AI provider configured");
    }
}
