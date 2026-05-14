package com.albertoreal.pokemongame.quiz;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Stateless quiz logic. The frontend keeps track of which questions have
 * been answered and accumulates the running score; the server simply
 * exposes the questions and validates each answer.
 */
@Service
public class QuizService {

    private final DailyQuizRepository quizRepo;
    private final DailyQuizQuestionRepository questionRepo;
    private final MultipleChoiceMatcher matcher;

    public QuizService(DailyQuizRepository quizRepo,
                       DailyQuizQuestionRepository questionRepo) {
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.matcher = new MultipleChoiceMatcher();
    }

    public QuizView todayQuiz() {
        var date = LocalDate.now();
        var quiz = quizRepo.findById(date).orElseThrow();
        var questions = questionRepo.findByQuizDateOrderByPositionAsc(date).stream()
            .map(q -> new QuizView.QuestionView(
                q.getId(), q.getPosition(), q.getQuestionText(), q.getOptions()))
            .toList();
        return new QuizView(quiz.getStatus().name(), questions);
    }

    public QuizAnswerResult answerByTranscript(Long questionId, String transcript) {
        var question = questionRepo.findById(questionId).orElseThrow();
        int selected = matcher.match(transcript, question.getOptions()).orElse(-1);
        return new QuizAnswerResult(
            selected == question.getCorrectOptionIndex(),
            selected,
            question.getCorrectOptionIndex()
        );
    }

    public QuizAnswerResult answerByIndex(Long questionId, int selectedIndex) {
        var question = questionRepo.findById(questionId).orElseThrow();
        return new QuizAnswerResult(
            selectedIndex == question.getCorrectOptionIndex(),
            selectedIndex,
            question.getCorrectOptionIndex()
        );
    }
}
