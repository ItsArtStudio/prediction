package org.java5thsem.predictions.provider;

import java.time.Instant;

public record ProviderFixture(
        long id,
        String round,
        Instant kickoffAt,
        String statusShort,
        String statusLong,
        Integer elapsedMinutes,
        long homeTeamId,
        String homeTeamName,
        String homeTeamLogo,
        long awayTeamId,
        String awayTeamName,
        String awayTeamLogo,
        Integer homeGoals,
        Integer awayGoals
) {
}
