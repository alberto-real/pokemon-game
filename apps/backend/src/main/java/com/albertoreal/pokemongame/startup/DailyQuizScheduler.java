package com.albertoreal.pokemongame.startup;

import com.albertoreal.pokemongame.quiz.QuizOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "pokemon-game.scheduler", name = "enabled", havingValue = "true")
public class DailyQuizScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyQuizScheduler.class);

    private final QuizOrchestrator orchestrator;

    public DailyQuizScheduler(QuizOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(cron = "${pokemon-game.scheduler.cron:0 0 6 * * *}",
               zone = "${pokemon-game.scheduler.zone:Europe/Madrid}")
    public void generateDaily() {
        var date = LocalDate.now();
        log.info("Scheduled generation for {}", date);
        orchestrator.ensureExists(date);
    }
}
