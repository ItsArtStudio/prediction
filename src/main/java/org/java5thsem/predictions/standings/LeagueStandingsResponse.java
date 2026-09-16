package org.java5thsem.predictions.standings;

import java.time.Instant;
import java.util.List;

public record LeagueStandingsResponse(
        String league,
        String leagueCode,
        int season,
        Instant updatedAt,
        List<StandingRowResponse> standings
) {
}
