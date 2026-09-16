package org.java5thsem.predictions.leaderboard;

import java.util.List;

public record LeaderboardResponse(ScoringRules scoring, List<LeaderboardEntry> entries) {
}
