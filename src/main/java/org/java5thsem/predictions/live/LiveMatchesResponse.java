package org.java5thsem.predictions.live;

import java.time.Instant;
import java.util.List;

public record LiveMatchesResponse(
        String league,
        String leagueCode,
        Instant refreshedAt,
        int refreshIntervalSeconds,
        List<LiveMatchSummary> matches
) {
}
