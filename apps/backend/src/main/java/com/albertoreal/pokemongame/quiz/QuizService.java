package com.albertoreal.pokemongame.quiz;

import com.albertoreal.pokemongame.common.CurrentUser;
import com.albertoreal.pokemongame.game.UserDailyAttempt;
import com.albertoreal.pokemongame.game.UserDailyAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {

    private final DailyQuizRepository quizRepo;
    private final DailyQuizQuestionRepository questionRepo;
    private final UserDailyAttemptRepository attemptRepo;
    private final MultipleChoiceMatcher matcher;
    private final CurrentUser currentUser;

    public QuizService(DailyQuizRepository quizRepo,
                       DailyQuizQuestionRepository questionRepo,
                       UserDailyAttemptRepository attemptRepo,
                       CurrentUser currentUser) {
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
        this.matcher = new MultipleChoiceMatcher();
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public QuizView todayQuiz() {
        var date = LocalDate.now();
        var quiz = quizRepo.findById(date).orElseThrow();
        var attempt = attemptRepo.findByUserIdAndDate(currentUser.userId(), date)
            .orElseGet(() -> new UserDailyAttempt(currentUser.userId(), date));

        List<String> answeredPositions = attempt.getQuizAnswers() == null ? List.of() : attempt.getQuizAnswers();
        List<Integer> answeredPosList = answeredPositions.stream()
            .map(a -> {
                try {
                    return Integer.parseInt(a.split(",")[0].split("=")[1]);
                } catch (Exception e) { return -1; }
            })
            .toList();

        var questions = questionRepo.findByQuizDateOrderByPositionAsc(date).stream()
            .filter(q -> !answeredPosList.contains(q.getPosition()))
            .map(q -> new QuizView.QuestionView(
                q.getId(), q.getPosition(), q.getQuestionText(), q.getOptions()))
            .toList();
        return new QuizView(quiz.getStatus().name(), questions);
    }

    @Transactional
    public QuizAnswerResult answerByTranscript(Long questionId, String transcript) {
        var question = questionRepo.findById(questionId).orElseThrow();
        int selected = matcher.match(transcript, question.getOptions()).orElse(-1);
        return answer(question, selected);
    }

    @Transactional
    public QuizAnswerResult answerByIndex(Long questionId, int selectedIndex) {
        var question = questionRepo.findById(questionId).orElseThrow();
        return answer(question, selectedIndex);
    }

    private QuizAnswerResult answer(DailyQuizQuestion question, int selected) {
        var date = question.getQuizDate();
        var attempt = attemptRepo.findByUserIdAndDate(currentUser.userId(), date)
            .orElseGet(() -> new UserDailyAttempt(currentUser.userId(), date));

        List<String> answers = new ArrayList<>(
            attempt.getQuizAnswers() == null ? List.of() : attempt.getQuizAnswers());

        // Check if already answered
        boolean alreadyAnswered = answers.stream().anyMatch(a -> a.startsWith("position=" + question.getPosition() + ","));
        if (alreadyAnswered) {
            // Return current state without adding new answer
            int score = 0;
            for (String a : answers) if (a.contains("correct=true")) score++;
            return new QuizAnswerResult(false, -1, question.getCorrectOptionIndex(),
                answers.size() >= questionRepo.findByQuizDateOrderByPositionAsc(date).size(),
                score, attempt.getTotalScore());
        }

        boolean correct = selected == question.getCorrectOptionIndex();
        answers.add("position=" + question.getPosition()
            + ",selected=" + selected + ",correct=" + correct);
        attempt.setQuizAnswers(answers);

        var allQuestions = questionRepo.findByQuizDateOrderByPositionAsc(date);
        boolean complete = answers.size() >= allQuestions.size();

        if (complete) {
            int score = 0;
            for (String a : answers) if (a.contains("correct=true")) score++;
            attempt.setQuizScore(score);
            int nameScore = attempt.getNameScore() == null ? 0 : attempt.getNameScore();
            attempt.setTotalScore(nameScore + score);
            attempt.setCompletedAt(OffsetDateTime.now());
        }
        attemptRepo.save(attempt);

        return new QuizAnswerResult(correct, selected, question.getCorrectOptionIndex(),
            complete, attempt.getQuizScore(), attempt.getTotalScore());
    }
}
