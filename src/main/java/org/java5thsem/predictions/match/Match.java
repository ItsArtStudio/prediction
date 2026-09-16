package org.java5thsem.predictions.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String homeTeam;

    @Column(nullable = false)
    private String awayTeam;

    @Column(nullable = false)
    private Instant kickoffAt;

    @Column(nullable = false)
    private Instant predictionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Column(nullable = false)
    private boolean predictionsEnabled;

    @Column
    private Integer resultHomeScore;

    @Column
    private Integer resultAwayScore;

    protected Match() {
    }

    public Match(
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            Instant predictionDeadline,
            MatchStatus status,
            boolean predictionsEnabled
    ) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffAt = kickoffAt;
        this.predictionDeadline = predictionDeadline;
        this.status = status;
        this.predictionsEnabled = predictionsEnabled;
    }

    public Long getId() {
        return id;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public Instant getKickoffAt() {
        return kickoffAt;
    }

    public Instant getPredictionDeadline() {
        return predictionDeadline;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public boolean isPredictionsEnabled() {
        return predictionsEnabled;
    }

    public boolean isAvailableForPredictions() {
        return predictionsEnabled && status == MatchStatus.SCHEDULED;
    }

    public boolean hasStarted(Instant now) {
        return status == MatchStatus.LIVE
                || status == MatchStatus.FINISHED
                || !now.isBefore(kickoffAt);
    }

    public boolean isAfterDeadline(Instant now) {
        return !now.isBefore(predictionDeadline);
    }

    public Integer getResultHomeScore() {
        return resultHomeScore;
    }

    public Integer getResultAwayScore() {
        return resultAwayScore;
    }

    public void complete(int homeScore, int awayScore) {
        this.resultHomeScore = homeScore;
        this.resultAwayScore = awayScore;
        this.status = MatchStatus.FINISHED;
        this.predictionsEnabled = false;
    }

    public boolean isEligibleForScoring() {
        return status == MatchStatus.FINISHED
                && resultHomeScore != null
                && resultAwayScore != null;
    }
}
