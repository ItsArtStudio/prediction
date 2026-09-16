package org.java5thsem.predictions.provider;

import java.time.Instant;

public record ProviderStanding(
        int position,
        long teamId,
        String teamName,
        String teamLogo,
        int points,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        Instant updatedAt
) {
}
