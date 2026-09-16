package org.java5thsem.predictions.matchweek;

import java.time.Instant;

public record MatchweekMatchResponse(
        long fixtureId,
        Instant kickoffAt,
        String status,
        String statusCode,
        String statusDetail,
        Integer elapsedMinutes,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        ScoreResponse score
) {
}
