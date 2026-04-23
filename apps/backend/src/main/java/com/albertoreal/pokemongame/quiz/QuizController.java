package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.quiz.dto.QuizAnswerRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game/today/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public QuizView today() {
        return quizService.todayQuiz();
    }

    @PostMapping("/answer/{questionId}")
    public QuizAnswerResult answer(@PathVariable Long questionId,
                                   @Valid @RequestBody QuizAnswerRequest req) {
        return quizService.answerByTranscript(questionId, req.transcript());
    }
}
