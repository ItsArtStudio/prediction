package org.java5thsem.predictions.leaderboard;

public record LeaderboardEntry(
        int rank,
        Long userId,
        String username,
        int points,
        int predictionsScored,
        int exactScores,
        int correctOutcomes
) {
}
