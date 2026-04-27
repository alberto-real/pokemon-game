package com.albertoreal.pokemongame.game;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_daily_attempt")
@IdClass(UserDailyAttempt.Key.class)
public class UserDailyAttempt {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "name_attempts_used", nullable = false)
    private int nameAttemptsUsed;

    @Column(name = "name_solved", nullable = false)
    private boolean nameSolved;

    @Column(name = "name_surrendered", nullable = false)
    private boolean nameSurrendered;

    @Column(name = "name_score")
    private Integer nameScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name_attempts", columnDefinition = "jsonb", nullable = false)
    private List<String> nameAttempts = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiz_answers", columnDefinition = "jsonb")
    private List<String> quizAnswers;

    @Column(name = "quiz_score")
    private Integer quizScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected UserDailyAttempt() {}

    public UserDailyAttempt(String userId, LocalDate date) {
        this.userId = userId;
        this.date = date;
    }

    public record Key(String userId, LocalDate date) implements Serializable {}

    public String getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public int getNameAttemptsUsed() { return nameAttemptsUsed; }
    public void setNameAttemptsUsed(int n) { this.nameAttemptsUsed = n; }
    public boolean isNameSolved() { return nameSolved; }
    public void setNameSolved(boolean v) { this.nameSolved = v; }
    public boolean isNameSurrendered() { return nameSurrendered; }
    public void setNameSurrendered(boolean v) { this.nameSurrendered = v; }
    public Integer getNameScore() { return nameScore; }
    public void setNameScore(Integer s) { this.nameScore = s; }
    public List<String> getNameAttempts() {
        if (nameAttempts == null) nameAttempts = new ArrayList<>();
        return nameAttempts;
    }
    public void setNameAttempts(List<String> a) { this.nameAttempts = a; }
    public List<String> getQuizAnswers() { return quizAnswers; }
    public void setQuizAnswers(List<String> a) { this.quizAnswers = a; }
    public Integer getQuizScore() { return quizScore; }
    public void setQuizScore(Integer s) { this.quizScore = s; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer s) { this.totalScore = s; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime c) { this.completedAt = c; }
}
