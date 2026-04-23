package com.albertoreal.pokemongame.quiz;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_quiz")
public class DailyQuiz {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizStatus status;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    protected DailyQuiz() {}

    public DailyQuiz(LocalDate date, QuizStatus status, OffsetDateTime startedAt) {
        this.date = date;
        this.status = status;
        this.startedAt = startedAt;
    }

    public LocalDate getDate() { return date; }
    public QuizStatus getStatus() { return status; }
    public void setStatus(QuizStatus s) { this.status = s; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String m) { this.statusMessage = m; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getReadyAt() { return readyAt; }
    public void setReadyAt(OffsetDateTime r) { this.readyAt = r; }
}
