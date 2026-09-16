package org.java5thsem.predictions.live;

import org.java5thsem.predictions.matchweek.MatchStatusMapper;
import org.java5thsem.predictions.matchweek.ScoreResponse;
import org.java5thsem.predictions.matchweek.TeamResponse;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.java5thsem.predictions.schedule.LeagueResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class LiveMatchService {

    static final String LEAGUE_NAME = "Premier League";
    static final String LEAGUE_CODE = "EPL";

    private final FootballDataProvider footballDataProvider;
    private final ApiFootballProperties properties;
    private final Clock clock;

    public LiveMatchService(
            FootballDataProvider footballDataProvider,
            ApiFootballProperties properties,
            Clock clock
    ) {
        this.footballDataProvider = footballDataProvider;
        this.properties = properties;
        this.clock = clock;
    }

    public LiveMatchesResponse listLiveMatches() {
        Instant refreshedAt = Instant.now(clock);
        List<LiveMatchSummary> matches = footballDataProvider.loadLivePremierLeagueMatches().stream()
                .sorted(Comparator.comparing(ProviderFixture::kickoffAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProviderFixture::id))
                .map(LiveMatchService::toSummary)
                .toList();
        return new LiveMatchesResponse(
                LEAGUE_NAME,
                LEAGUE_CODE,
                refreshedAt,
                refreshIntervalSeconds(),
                matches
        );
    }

    public MatchDetailsResponse getMatchDetails(long fixtureId) {
        ProviderMatchDetails details = footballDataProvider.loadPremierLeagueMatchDetails(fixtureId);
        ProviderFixture fixture = details.fixture();
        int interval = MatchStatusMapper.isLive(fixture.statusShort())
                ? refreshIntervalSeconds()
                : (int) Math.max(1, properties.cacheTtl().toSeconds());
        return new MatchDetailsResponse(
                fixture.id(),
                fixture.kickoffAt(),
                MatchStatusMapper.matchStatus(fixture.statusShort()),
                fixture.statusShort(),
                fixture.statusLong(),
                fixture.elapsedMinutes(),
                details.extraMinutes(),
                details.referee(),
                new VenueResponse(details.venueName(), details.venueCity()),
                new LeagueResponse(
                        details.leagueName() == null ? LEAGUE_NAME : details.leagueName(),
                        LEAGUE_CODE,
                        fixture.round()
                ),
                new TeamResponse(fixture.homeTeamId(), fixture.homeTeamName(), fixture.homeTeamLogo()),
                new TeamResponse(fixture.awayTeamId(), fixture.awayTeamName(), fixture.awayTeamLogo()),
                new ScoreResponse(fixture.homeGoals(), fixture.awayGoals()),
                toScoreBreakdown(details.scoreBreakdown()),
                details.events().stream().map(LiveMatchService::toEvent).toList(),
                details.lineups().stream().map(LiveMatchService::toLineup).toList(),
                details.statistics().stream().map(LiveMatchService::toStatistics).toList(),
                Instant.now(clock),
                interval
        );
    }

    private int refreshIntervalSeconds() {
        Duration ttl = properties.liveCacheTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return 60;
        }
        return (int) ttl.toSeconds();
    }

    private static LiveMatchSummary toSummary(ProviderFixture fixture) {
        return new LiveMatchSummary(
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

    private static ScoreBreakdownResponse toScoreBreakdown(ProviderMatchDetails.ScoreBreakdown score) {
        if (score == null) {
            return null;
        }
        return new ScoreBreakdownResponse(
                score.halftimeHome(),
                score.halftimeAway(),
                score.fulltimeHome(),
                score.fulltimeAway(),
                score.extraTimeHome(),
                score.extraTimeAway(),
                score.penaltyHome(),
                score.penaltyAway()
        );
    }

    private static MatchEventResponse toEvent(ProviderMatchDetails.Event event) {
        return new MatchEventResponse(
                event.elapsedMinutes(),
                event.extraMinutes(),
                event.teamId(),
                event.teamName(),
                event.playerId(),
                event.playerName(),
                event.assistId(),
                event.assistName(),
                event.type(),
                event.detail(),
                event.comments()
        );
    }

    private static LineupResponse toLineup(ProviderMatchDetails.Lineup lineup) {
        return new LineupResponse(
                lineup.teamId(),
                lineup.teamName(),
                lineup.teamLogo(),
                lineup.formation(),
                lineup.coachName(),
                lineup.startXi().stream().map(LiveMatchService::toPlayer).toList(),
                lineup.substitutes().stream().map(LiveMatchService::toPlayer).toList()
        );
    }

    private static LineupPlayerResponse toPlayer(ProviderMatchDetails.LineupPlayer player) {
        return new LineupPlayerResponse(
                player.id(),
                player.name(),
                player.number(),
                player.position(),
                player.grid()
        );
    }

    private static TeamStatisticsResponse toStatistics(ProviderMatchDetails.TeamStatistics statistics) {
        return new TeamStatisticsResponse(
                statistics.teamId(),
                statistics.teamName(),
                statistics.statistics().stream()
                        .map(stat -> new TeamStatisticsResponse.StatisticResponse(stat.type(), stat.value()))
                        .toList()
        );
    }
}
