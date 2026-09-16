package org.java5thsem.predictions.live;

import org.java5thsem.predictions.matchweek.ScoreResponse;
import org.java5thsem.predictions.matchweek.TeamResponse;
import org.java5thsem.predictions.schedule.LeagueResponse;

import java.time.Instant;

public record LiveMatchSummary(
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
