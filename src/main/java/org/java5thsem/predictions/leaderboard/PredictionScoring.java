package org.java5thsem.predictions.leaderboard;

final class PredictionScoring {

    static final int EXACT_SCORE_POINTS = 3;
    static final int CORRECT_OUTCOME_POINTS = 1;

    private PredictionScoring() {
    }

    static int points(int predictedHome, int predictedAway, int actualHome, int actualAway) {
        if (predictedHome == actualHome && predictedAway == actualAway) {
            return EXACT_SCORE_POINTS;
        }
        if (outcome(predictedHome, predictedAway) == outcome(actualHome, actualAway)) {
            return CORRECT_OUTCOME_POINTS;
        }
        return 0;
    }

    private static int outcome(int home, int away) {
        return Integer.compare(home, away);
    }
}
