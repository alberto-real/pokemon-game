package com.albertoreal.pokemongame.startup;

import com.albertoreal.pokemongame.quiz.QuizOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailyQuizStartupRunner {

    private static final Logger log = LoggerFactory.getLogger(DailyQuizStartupRunner.class);

    private final QuizOrchestrator orchestrator;

    public DailyQuizStartupRunner(QuizOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        var date = LocalDate.now();
        log.info("Ensuring quiz exists for {}", date);
        try {
            orchestrator.ensureExists(date);
            log.info("Daily quiz ensured for {}", date);
        } catch (Exception e) {
            log.error("Failed to ensure today's quiz at startup", e);
        }
    }
}
