package com.albertoreal.pokemongame.ai;

import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.EvolutionChainDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("openRouterQuizGenerator")
@ConditionalOnBean(name = "openRouterChatModel")
public class OpenRouterQuizGenerator implements QuizGenerator {

    private final ChatClient client;
    private final PokeApiClient pokeApi;
    private final RagContextBuilder rag;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouterQuizGenerator(@Qualifier("openRouterChatModel") OpenAiChatModel openRouter,
                                   PokeApiClient pokeApi,
                                   RagContextBuilder rag) {
        this.client = ChatClient.create(openRouter);
        this.pokeApi = pokeApi;
        this.rag = rag;
    }

    @Override
    public List<GeneratedQuestion> generate(PokemonDto pokemon, PokemonSpeciesDto species) {
        EvolutionChainDto chain = null;
        if (species != null && species.evolutionChainUrl() != null) {
            try { chain = pokeApi.getEvolutionChain(species.evolutionChainUrl()); }
            catch (Exception ignore) { /* non-fatal */ }
        }
        String ragCtx = rag.build(pokemon, species, chain);

        String content;
        try {
            content = client.prompt()
                .system(QuizPrompt.SYSTEM)
                .user(QuizPrompt.user(ragCtx))
                .call()
                .content();
        } catch (Exception e) {
            throw new QuizGenerationException("OpenRouter call failed: " + e.getMessage(), e);
        }

        String json = stripCodeFences(content);
        try {
            GeneratedQuiz quiz = mapper.readValue(json, GeneratedQuiz.class);
            if (quiz.questions() == null || quiz.questions().size() != 5) {
                throw new QuizGenerationException(
                    "Expected 5 questions, got " + (quiz.questions() == null ? 0 : quiz.questions().size()));
            }
            for (var q : quiz.questions()) {
                if (q.options() == null || q.options().size() != 4) {
                    throw new QuizGenerationException("Each question must have 4 options");
                }
                if (q.correctIndex() < 0 || q.correctIndex() > 3) {
                    throw new QuizGenerationException("correctIndex out of range: " + q.correctIndex());
                }
            }
            return quiz.questions();
        } catch (QuizGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new QuizGenerationException(
                "Failed to parse OpenRouter JSON: " + e.getMessage() + " (raw: "
                    + (json.length() > 200 ? json.substring(0, 200) + "..." : json) + ")", e);
        }
    }

    private static String stripCodeFences(String text) {
        if (text == null) return "";
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
