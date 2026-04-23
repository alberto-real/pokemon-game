package com.albertoreal.pokemongame.game;

import com.albertoreal.pokemongame.game.dto.AttemptRequest;
import com.albertoreal.pokemongame.quiz.QuizOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final QuizOrchestrator orchestrator;

    public GameController(GameService gameService, QuizOrchestrator orchestrator) {
        this.gameService = gameService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/today")
    public GameState today() {
        orchestrator.ensureExists(LocalDate.now());
        return gameService.today();
    }

    @PostMapping("/today/attempt")
    public AttemptResult attempt(@Valid @RequestBody AttemptRequest req) {
        return gameService.attempt(req.transcript());
    }

    @PostMapping("/today/surrender")
    public GameState surrender() {
        return gameService.surrender();
    }
}
