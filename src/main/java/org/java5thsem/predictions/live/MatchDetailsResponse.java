package org.java5thsem.predictions.live;

import org.java5thsem.predictions.matchweek.ScoreResponse;
import org.java5thsem.predictions.matchweek.TeamResponse;
import org.java5thsem.predictions.schedule.LeagueResponse;

import java.time.Instant;
import java.util.List;

public record MatchDetailsResponse(
        long fixtureId,
        Instant kickoffAt,
        String status,
        String statusCode,
        String statusDetail,
        Integer elapsedMinutes,
        Integer extraMinutes,
        String referee,
        VenueResponse venue,
        LeagueResponse league,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        ScoreResponse score,
        ScoreBreakdownResponse scoreBreakdown,
        List<MatchEventResponse> events,
        List<LineupResponse> lineups,
        List<TeamStatisticsResponse> statistics,
        Instant refreshedAt,
        int refreshIntervalSeconds
) {
}
