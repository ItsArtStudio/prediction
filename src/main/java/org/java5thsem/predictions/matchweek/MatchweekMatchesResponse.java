package org.java5thsem.predictions.matchweek;

import java.util.List;

public record MatchweekMatchesResponse(
        String league,
        String leagueCode,
        int season,
        MatchweekSummary matchweek,
        List<MatchweekMatchResponse> matches
) {
}
