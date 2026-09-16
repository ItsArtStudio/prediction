package org.java5thsem.predictions.matchweek;

import java.time.Instant;

public record MatchweekSummary(
        int number,
        String name,
        String status,
        Instant startAt,
        Instant endAt,
        int matchCount
) {
}
