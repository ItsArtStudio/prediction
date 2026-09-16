package org.java5thsem.predictions.matchweek;

import java.util.List;

public record MatchweekListResponse(
        String league,
        String leagueCode,
        int season,
        Integer currentMatchweek,
        List<MatchweekSummary> matchweeks
) {
}
