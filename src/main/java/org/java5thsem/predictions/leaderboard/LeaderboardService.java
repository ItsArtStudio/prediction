package org.java5thsem.predictions.leaderboard;

import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.match.MatchStatus;
import org.java5thsem.predictions.prediction.Prediction;
import org.java5thsem.predictions.prediction.PredictionRepository;
import org.java5thsem.predictions.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaderboardService {

    private final PredictionRepository predictionRepository;

    public LeaderboardService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard() {
        List<Prediction> predictions = predictionRepository.findAllForScoring(MatchStatus.FINISHED);
        Map<Long, ScoreAccumulator> byUser = new LinkedHashMap<>();

        for (Prediction prediction : predictions) {
            Match match = prediction.getMatch();
            if (!match.isEligibleForScoring()) {
                continue;
            }
            User user = prediction.getUser();
            int points = PredictionScoring.points(
                    prediction.getHomeScore(),
                    prediction.getAwayScore(),
                    match.getResultHomeScore(),
                    match.getResultAwayScore()
            );
            byUser.computeIfAbsent(user.getId(), id -> new ScoreAccumulator(user))
                    .add(points);
        }

        List<ScoreAccumulator> ranked = byUser.values().stream()
                .sorted(Comparator
                        .comparingInt(ScoreAccumulator::points).reversed()
                        .thenComparing(Comparator.comparingInt(ScoreAccumulator::exactScores).reversed())
                        .thenComparing(Comparator.comparingInt(ScoreAccumulator::correctOutcomes).reversed())
                        .thenComparing(ScoreAccumulator::username, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LeaderboardEntry> entries = new ArrayList<>(ranked.size());
        int rank = 1;
        for (int i = 0; i < ranked.size(); i++) {
            ScoreAccumulator current = ranked.get(i);
            if (i > 0 && current.points() < ranked.get(i - 1).points()) {
                rank = i + 1;
            }
            entries.add(current.toEntry(rank));
        }

        return new LeaderboardResponse(ScoringRules.defaults(), List.copyOf(entries));
    }

    private static final class ScoreAccumulator {
        private final User user;
        private int points;
        private int predictionsScored;
        private int exactScores;
        private int correctOutcomes;

        private ScoreAccumulator(User user) {
            this.user = user;
        }

        private void add(int awardedPoints) {
            predictionsScored++;
            points += awardedPoints;
            if (awardedPoints == PredictionScoring.EXACT_SCORE_POINTS) {
                exactScores++;
            } else if (awardedPoints == PredictionScoring.CORRECT_OUTCOME_POINTS) {
                correctOutcomes++;
            }
        }

        private int points() {
            return points;
        }

        private int exactScores() {
            return exactScores;
        }

        private int correctOutcomes() {
            return correctOutcomes;
        }

        private String username() {
            return user.getUsername();
        }

        private LeaderboardEntry toEntry(int rank) {
            return new LeaderboardEntry(
                    rank,
                    user.getId(),
                    user.getUsername(),
                    points,
                    predictionsScored,
                    exactScores,
                    correctOutcomes
            );
        }
    }
}
