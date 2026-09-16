package org.java5thsem.predictions.schedule;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.matchweek.MatchStatusMapper;
import org.java5thsem.predictions.matchweek.ScoreResponse;
import org.java5thsem.predictions.matchweek.TeamResponse;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class MatchScheduleService {

    static final String LEAGUE_NAME = "Premier League";
    static final String LEAGUE_CODE = "EPL";

    private static final Set<String> UPCOMING_STATUSES = Set.of("SCHEDULED", "POSTPONED");

    private final FootballDataProvider footballDataProvider;
    private final ApiFootballProperties properties;
    private final Clock clock;

    public MatchScheduleService(
            FootballDataProvider footballDataProvider,
            ApiFootballProperties properties,
            Clock clock
    ) {
        this.footballDataProvider = footballDataProvider;
        this.properties = properties;
        this.clock = clock;
    }

    public MatchScheduleResponse getSchedule(Integer season, LocalDate date, LocalDate from, LocalDate to) {
        validateFilters(date, from, to);
        int resolvedSeason = season == null ? properties.season() : season;
        LocalDate rangeStart = date != null ? date : from;
        LocalDate rangeEnd = date != null ? date : to;
        boolean upcomingOnly = rangeStart == null && rangeEnd == null;
        Instant now = Instant.now(clock);

        List<ScheduledMatchResponse> matches = footballDataProvider.loadPremierLeagueSeason(resolvedSeason)
                .fixtures()
                .stream()
                .filter(fixture -> matchesDateFilter(fixture, rangeStart, rangeEnd))
                .filter(fixture -> !upcomingOnly || isUpcoming(fixture, now))
                .sorted(Comparator.comparing(ProviderFixture::kickoffAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProviderFixture::id))
                .map(MatchScheduleService::toMatch)
                .toList();

        return new MatchScheduleResponse(
                LEAGUE_NAME,
                LEAGUE_CODE,
                resolvedSeason,
                date,
                from,
                to,
                upcomingOnly,
                matches
        );
    }

    private static void validateFilters(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && (from != null || to != null)) {
            throw ApiException.invalidDateRange("Use either date or from/to, not both");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.invalidDateRange("from must be on or before to");
        }
    }

    private static boolean matchesDateFilter(ProviderFixture fixture, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (fixture.kickoffAt() == null) {
            return false;
        }
        LocalDate kickoffDate = fixture.kickoffAt().atZone(ZoneOffset.UTC).toLocalDate();
        if (from != null && kickoffDate.isBefore(from)) {
            return false;
        }
        return to == null || !kickoffDate.isAfter(to);
    }

    private static boolean isUpcoming(ProviderFixture fixture, Instant now) {
        String status = MatchStatusMapper.matchStatus(fixture.statusShort());
        if (!UPCOMING_STATUSES.contains(status)) {
            return false;
        }
        return fixture.kickoffAt() == null || !fixture.kickoffAt().isBefore(now);
    }

    private static ScheduledMatchResponse toMatch(ProviderFixture fixture) {
        return new ScheduledMatchResponse(
                fixture.id(),
                fixture.kickoffAt(),
                MatchStatusMapper.matchStatus(fixture.statusShort()),
                fixture.statusShort(),
                fixture.statusLong(),
                fixture.elapsedMinutes(),
                new LeagueResponse(LEAGUE_NAME, LEAGUE_CODE, fixture.round()),
                new TeamResponse(fixture.homeTeamId(), fixture.homeTeamName(), fixture.homeTeamLogo()),
                new TeamResponse(fixture.awayTeamId(), fixture.awayTeamName(), fixture.awayTeamLogo()),
                new ScoreResponse(fixture.homeGoals(), fixture.awayGoals())
        );
    }
}
