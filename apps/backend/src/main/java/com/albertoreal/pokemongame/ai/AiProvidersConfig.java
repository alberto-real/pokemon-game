package com.albertoreal.pokemongame.ai;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProvidersConfig {

    @Bean(name = "groqChatModel")
    @ConditionalOnProperty(prefix = "pokemon-game.ai.groq", name = "enabled", havingValue = "true")
    public OpenAiChatModel groqChatModel(AiProvidersProperties props) {
        var api = OpenAiApi.builder()
            .baseUrl("https://api.groq.com/openai")
            .apiKey(props.groq().apiKey())
            .build();
        var options = OpenAiChatOptions.builder()
            .model(props.groq().model())
            .temperature(0.2)
            .build();
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(options)
            .build();
    }

    @Bean(name = "openRouterChatModel")
    @ConditionalOnProperty(prefix = "pokemon-game.ai.openrouter", name = "enabled", havingValue = "true")
    public OpenAiChatModel openRouterChatModel(AiProvidersProperties props) {
        var api = OpenAiApi.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .apiKey(props.openrouter().apiKey())
            .build();
        var options = OpenAiChatOptions.builder()
            .model(props.openrouter().model())
            .temperature(0.2)
            .build();
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(options)
            .build();
    }
}
