package com.albertoreal.pokemongame.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-game.ai")
public record AiProvidersProperties(
    String provider,
    Ollama ollama,
    Groq groq,
    OpenRouter openrouter
) {
    public AiProvidersProperties {
        if (provider == null || provider.isBlank()) provider = "stub";
        if (ollama == null) ollama = new Ollama("http://localhost:11434", "llama3.2:latest");
        if (groq == null) groq = new Groq(null, "openai/gpt-oss-20b", false);
        if (openrouter == null) openrouter = new OpenRouter(null,
            "openai/gpt-oss-20b:free", false);
    }

    public record Ollama(String baseUrl, String model) {
        public Ollama {
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
            if (model == null || model.isBlank()) model = "llama3.2:latest";
        }
    }

    public record Groq(String apiKey, String model, boolean enabled) {
        public Groq {
            if (model == null || model.isBlank()) model = "openai/gpt-oss-20b";
        }
    }

    public record OpenRouter(String apiKey, String model, boolean enabled) {
        public OpenRouter {
            if (model == null || model.isBlank()) model = "openai/gpt-oss-20b:free";
        }
    }
}
