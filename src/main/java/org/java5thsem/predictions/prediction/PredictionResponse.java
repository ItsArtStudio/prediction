package org.java5thsem.predictions.prediction;

import java.time.Instant;

public record PredictionResponse(
        Long id,
        Long userId,
        Long matchId,
        int homeScore,
        int awayScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static PredictionResponse from(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getUser().getId(),
                prediction.getMatch().getId(),
                prediction.getHomeScore(),
                prediction.getAwayScore(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }
}
