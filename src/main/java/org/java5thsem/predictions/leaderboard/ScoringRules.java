package org.java5thsem.predictions.leaderboard;

public record ScoringRules(int exactScorePoints, int correctOutcomePoints) {

    static ScoringRules defaults() {
        return new ScoringRules(
                PredictionScoring.EXACT_SCORE_POINTS,
                PredictionScoring.CORRECT_OUTCOME_POINTS
        );
    }
}
