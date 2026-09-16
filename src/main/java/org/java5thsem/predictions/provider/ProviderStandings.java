package org.java5thsem.predictions.provider;

import java.time.Instant;
import java.util.List;

public record ProviderStandings(
        int leagueId,
        String leagueName,
        int season,
        Instant updatedAt,
        List<ProviderStanding> table
) {
}
