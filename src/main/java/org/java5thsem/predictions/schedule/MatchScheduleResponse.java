package org.java5thsem.predictions.schedule;

import java.time.LocalDate;
import java.util.List;

public record MatchScheduleResponse(
        String league,
        String leagueCode,
        int season,
        LocalDate date,
        LocalDate from,
        LocalDate to,
        boolean upcomingOnly,
        List<ScheduledMatchResponse> matches
) {
}
