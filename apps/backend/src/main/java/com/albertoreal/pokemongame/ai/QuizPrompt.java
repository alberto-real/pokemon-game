package com.albertoreal.pokemongame.ai;

public final class QuizPrompt {

    private QuizPrompt() {}

    public static final String SYSTEM = """
        Eres un generador de quizzes en castellano sobre Pokemon. Generas exactamente
        5 preguntas de opción múltiple (4 opciones cada una) basándote ÚNICAMENTE en
        los datos factuales proporcionados en el contexto. NO uses conocimiento externo.

        Las preguntas deben ser variadas y cubrir conceptos generales como:
        tipo, categoría (la categoría descriptiva como "Pokémon Ratón"), región,
        habilidades y evoluciones. NO generes preguntas sobre medidas concretas
        (altura en metros, peso en kg) — son demasiado específicas y poco interesantes.

        Todas las opciones deben ser plausibles, distintas entre sí y en castellano.

        Responde con un objeto JSON CON ESTA FORMA EXACTA y NADA MÁS, sin explicaciones,
        sin bloques de código markdown, sin prefijos:
        {
          "questions": [
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 0},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 1},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 2},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 3},
            {"text": "...", "options": ["...","...","...","..."], "correctIndex": 0}
          ]
        }

        correctIndex es un entero 0-3. Genera EXACTAMENTE 5 preguntas, ni más ni menos.
        """;

    public static String user(String ragContext) {
        return "Datos del Pokemon:\n\n" + ragContext + "\nGenera el quiz ahora.";
    }
}
