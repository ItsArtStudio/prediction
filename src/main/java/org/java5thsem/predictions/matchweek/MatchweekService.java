package org.java5thsem.predictions.matchweek;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MatchweekService {

    static final String LEAGUE_NAME = "Premier League";
    static final String LEAGUE_CODE = "EPL";

    private static final Pattern ROUND_NUMBER = Pattern.compile("(\\d+)\\s*$");

    private final FootballDataProvider footballDataProvider;
    private final ApiFootballProperties properties;

    public MatchweekService(FootballDataProvider footballDataProvider, ApiFootballProperties properties) {
        this.footballDataProvider = footballDataProvider;
        this.properties = properties;
    }

    public MatchweekListResponse listMatchweeks(Integer season) {
        int resolvedSeason = resolveSeason(season);
        SeasonSchedule schedule = footballDataProvider.loadPremierLeagueSeason(resolvedSeason);
        List<MatchweekSummary> matchweeks = summaries(schedule);
        Integer current = matchweeks.stream()
                .filter(week -> "LIVE".equals(week.status()) || "IN_PROGRESS".equals(week.status()))
                .map(MatchweekSummary::number)
                .findFirst()
                .orElseGet(() -> matchweeks.stream()
                        .filter(week -> "SCHEDULED".equals(week.status()))
                        .map(MatchweekSummary::number)
                        .findFirst()
                        .orElse(matchweeks.isEmpty() ? null : matchweeks.getLast().number()));
        return new MatchweekListResponse(LEAGUE_NAME, LEAGUE_CODE, resolvedSeason, current, matchweeks);
    }

    public MatchweekMatchesResponse listMatches(int matchweek, Integer season) {
        int resolvedSeason = resolveSeason(season);
        SeasonSchedule schedule = footballDataProvider.loadPremierLeagueSeason(resolvedSeason);
        List<MatchweekSummary> matchweeks = summaries(schedule);
        MatchweekSummary summary = matchweeks.stream()
                .filter(week -> week.number() == matchweek)
                .findFirst()
                .orElseThrow(() -> ApiException.matchweekNotFound(matchweek, resolvedSeason));
        List<MatchweekMatchResponse> matches = fixturesFor(schedule, summary.name()).stream()
                .sorted(Comparator.comparing(ProviderFixture::kickoffAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProviderFixture::id))
                .map(MatchweekService::toMatch)
                .toList();
        return new MatchweekMatchesResponse(LEAGUE_NAME, LEAGUE_CODE, resolvedSeason, summary, matches);
    }

    private int resolveSeason(Integer season) {
        return season == null ? properties.season() : season;
    }

    private static List<MatchweekSummary> summaries(SeasonSchedule schedule) {
        Map<String, List<ProviderFixture>> byRound = new LinkedHashMap<>();
        for (String round : schedule.rounds()) {
            byRound.put(round, new ArrayList<>());
        }
        for (ProviderFixture fixture : schedule.fixtures()) {
            if (fixture.round() == null) {
                continue;
            }
            byRound.computeIfAbsent(fixture.round(), key -> new ArrayList<>()).add(fixture);
        }
        List<MatchweekSummary> summaries = new ArrayList<>();
        int fallbackNumber = 1;
        for (Map.Entry<String, List<ProviderFixture>> entry : byRound.entrySet()) {
            int number = parseRoundNumber(entry.getKey()).orElse(fallbackNumber);
            summaries.add(toSummary(number, entry.getKey(), entry.getValue()));
            fallbackNumber = number + 1;
        }
        summaries.sort(Comparator.comparingInt(MatchweekSummary::number));
        return List.copyOf(summaries);
    }

    private static List<ProviderFixture> fixturesFor(SeasonSchedule schedule, String roundName) {
        return schedule.fixtures().stream()
                .filter(fixture -> roundName.equals(fixture.round()))
                .toList();
    }

    private static MatchweekSummary toSummary(int number, String name, List<ProviderFixture> fixtures) {
        Instant startAt = fixtures.stream()
                .map(ProviderFixture::kickoffAt)
                .filter(instant -> instant != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        Instant endAt = fixtures.stream()
                .map(ProviderFixture::kickoffAt)
                .filter(instant -> instant != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new MatchweekSummary(
                number,
                name,
                MatchStatusMapper.matchweekStatus(fixtures),
                startAt,
                endAt,
                fixtures.size()
        );
    }

    private static MatchweekMatchResponse toMatch(ProviderFixture fixture) {
        return new MatchweekMatchResponse(
                fixture.id(),
                fixture.kickoffAt(),
                MatchStatusMapper.matchStatus(fixture.statusShort()),
                fixture.statusShort(),
                fixture.statusLong(),
                fixture.elapsedMinutes(),
                new TeamResponse(fixture.homeTeamId(), fixture.homeTeamName(), fixture.homeTeamLogo()),
                new TeamResponse(fixture.awayTeamId(), fixture.awayTeamName(), fixture.awayTeamLogo()),
                new ScoreResponse(fixture.homeGoals(), fixture.awayGoals())
        );
    }

    static Optional<Integer> parseRoundNumber(String round) {
        if (round == null) {
            return Optional.empty();
        }
        Matcher matcher = ROUND_NUMBER.matcher(round);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }
}
