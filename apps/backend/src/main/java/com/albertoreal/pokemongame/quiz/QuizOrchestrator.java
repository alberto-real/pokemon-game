package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.ai.GeneratedQuestion;
import com.albertoreal.pokemongame.ai.QuizGenerator;
import com.albertoreal.pokemongame.game.DailyPokemon;
import com.albertoreal.pokemongame.game.DailyPokemonRepository;
import com.albertoreal.pokemongame.game.PokemonNameCatalog;
import com.albertoreal.pokemongame.game.PokemonSelector;
import com.albertoreal.pokemongame.pokeapi.PokeApiClient;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonDto;
import com.albertoreal.pokemongame.pokeapi.dto.PokemonSpeciesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QuizOrchestrator.class);

    private final DailyPokemonRepository pokemonRepo;
    private final DailyQuizRepository quizRepo;
    private final DailyQuizQuestionRepository questionRepo;
    private final PokemonSelector selector;
    private final PokeApiClient pokeApi;
    private final QuizGenerator quizGenerator;
    private final PokemonNameCatalog catalog;

    public QuizOrchestrator(DailyPokemonRepository pokemonRepo,
                            DailyQuizRepository quizRepo,
                            DailyQuizQuestionRepository questionRepo,
                            PokemonSelector selector,
                            PokeApiClient pokeApi,
                            QuizGenerator quizGenerator,
                            PokemonNameCatalog catalog) {
        this.pokemonRepo = pokemonRepo;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.selector = selector;
        this.pokeApi = pokeApi;
        this.quizGenerator = quizGenerator;
        this.catalog = catalog;
    }

    @Transactional
    public void ensureExists(LocalDate date) {
        if (quizRepo.findById(date).isPresent()) return;
        try {
            generateForDate(date);
        } catch (DataIntegrityViolationException conflict) {
            log.info("Concurrent generation detected for {}, skipping", date);
        }
    }

    protected void generateForDate(LocalDate date) {
        var used = pokemonRepo.findAll().stream()
            .map(DailyPokemon::getPokemonId)
            .collect(Collectors.toUnmodifiableSet());
        int pokemonId = selector.pick(used);

        PokemonDto pokemon = pokeApi.getPokemon(pokemonId);
        PokemonSpeciesDto species = pokeApi.getSpecies(pokemonId);
        String nameEs = species.nameInLanguage("es");
        if (nameEs == null) nameEs = pokemon.name();
        String imageUrl = pokemon.officialArtworkUrl();
        if (imageUrl == null) imageUrl = "";

        pokemonRepo.save(new DailyPokemon(
            date, pokemonId, pokemon.name(), nameEs, imageUrl, OffsetDateTime.now()));
        catalog.register(pokemon.name());

        var quiz = new DailyQuiz(date, QuizStatus.GENERATING_QUIZ, OffsetDateTime.now());
        quizRepo.save(quiz);

        List<GeneratedQuestion> generated = quizGenerator.generate(pokemon, species);
        for (int i = 0; i < generated.size(); i++) {
            var g = generated.get(i);
            questionRepo.save(new DailyQuizQuestion(
                date, i + 1, g.text(), g.options(), g.correctIndex()));
        }

        quiz.setStatus(QuizStatus.READY);
        quiz.setReadyAt(OffsetDateTime.now());
        quizRepo.save(quiz);
    }
}
