package org.java5thsem.predictions.schedule;

import org.java5thsem.predictions.matchweek.ScoreResponse;
import org.java5thsem.predictions.matchweek.TeamResponse;

import java.time.Instant;

public record ScheduledMatchResponse(
        long fixtureId,
        Instant kickoffAt,
        String status,
        String statusCode,
        String statusDetail,
        Integer elapsedMinutes,
        LeagueResponse league,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        ScoreResponse score
) {
}
