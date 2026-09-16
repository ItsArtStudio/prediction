package org.java5thsem.predictions.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "predictions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_predictions_user_match",
                columnNames = {"user_id", "match_id"}
        )
)
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private int homeScore;

    @Column(nullable = false)
    private int awayScore;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected Prediction() {
    }

    public Prediction(User user, Match match, int homeScore, int awayScore) {
        this.user = user;
        this.match = match;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Match getMatch() {
        return match;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateScore(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
