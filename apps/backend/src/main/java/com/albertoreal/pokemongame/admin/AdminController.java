package com.albertoreal.pokemongame.admin;

import com.albertoreal.pokemongame.game.UserDailyAttemptRepository;
import com.albertoreal.pokemongame.quiz.DailyQuizQuestionRepository;
import com.albertoreal.pokemongame.quiz.DailyQuizRepository;
import com.albertoreal.pokemongame.quiz.QuizOrchestrator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quiz")
public class AdminController {

    private final QuizOrchestrator orchestrator;
    private final DailyQuizRepository quizRepo;
    private final DailyQuizQuestionRepository questionRepo;
    private final UserDailyAttemptRepository attemptRepo;

    public AdminController(QuizOrchestrator orchestrator,
                           DailyQuizRepository quizRepo,
                           DailyQuizQuestionRepository questionRepo,
                           UserDailyAttemptRepository attemptRepo) {
        this.orchestrator = orchestrator;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var d = date == null ? LocalDate.now() : date;
        orchestrator.ensureExists(d);
        var quiz = quizRepo.findById(d).orElseThrow();
        return Map.of("date", d.toString(), "status", quiz.getStatus().name());
    }

    @PostMapping("/regenerate")
    @Transactional
    public Map<String, Object> regenerate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // Delete user results
        attemptRepo.deleteByDate(date);

        // Delete questions and quiz (daily_pokemon row persists - selector will avoid repeating)
        for (var q : questionRepo.findByQuizDateOrderByPositionAsc(date)) {
            questionRepo.deleteById(q.getId());
        }
        quizRepo.deleteById(date);
        orchestrator.ensureExists(date);
        var quiz = quizRepo.findById(date).orElseThrow();
        return Map.of("date", date.toString(), "status", quiz.getStatus().name());
    }

    @GetMapping("/status")
    public Map<String, Object> status(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var quiz = quizRepo.findById(date).orElseThrow();
        return Map.of(
            "date", date.toString(),
            "status", quiz.getStatus().name(),
            "message", quiz.getStatusMessage() == null ? "" : quiz.getStatusMessage()
        );
    }
}
