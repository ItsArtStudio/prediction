package org.java5thsem.predictions.leaderboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionScoringTests {

    @Test
    void exactScoreAwardsThreePoints() {
        assertThat(PredictionScoring.points(2, 1, 2, 1)).isEqualTo(3);
        assertThat(PredictionScoring.points(0, 0, 0, 0)).isEqualTo(3);
    }

    @Test
    void correctOutcomeAwardsOnePoint() {
        assertThat(PredictionScoring.points(3, 1, 2, 0)).isEqualTo(1);
        assertThat(PredictionScoring.points(0, 2, 1, 3)).isEqualTo(1);
        assertThat(PredictionScoring.points(1, 1, 0, 0)).isEqualTo(1);
    }

    @Test
    void wrongOutcomeAwardsZero() {
        assertThat(PredictionScoring.points(1, 0, 0, 1)).isEqualTo(0);
        assertThat(PredictionScoring.points(0, 0, 2, 1)).isEqualTo(0);
    }
}
