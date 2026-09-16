package org.java5thsem.predictions.live;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveMatchServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-07T15:00:00Z");
    private final ApiFootballProperties properties = new ApiFootballProperties(
            "https://v3.football.api-sports.io",
            "test-key",
            39,
            2026,
            Duration.ofMinutes(2),
            Duration.ofMinutes(1)
    );
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void listsLiveMatchesWithScoreAndMinute() {
        FootballDataProvider provider = stub(
                List.of(fixture(10, "2H", 67, 2, 1)),
                null
        );
        LiveMatchService service = new LiveMatchService(provider, properties, clock);

        LiveMatchesResponse response = service.listLiveMatches();

        assertThat(response.leagueCode()).isEqualTo("EPL");
        assertThat(response.refreshIntervalSeconds()).isEqualTo(60);
        assertThat(response.refreshedAt()).isEqualTo(NOW);
        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().getFirst().status()).isEqualTo("LIVE");
        assertThat(response.matches().getFirst().elapsedMinutes()).isEqualTo(67);
        assertThat(response.matches().getFirst().score().home()).isEqualTo(2);
        assertThat(response.matches().getFirst().homeTeam().name()).isEqualTo("Arsenal");
    }

    @Test
    void returnsDetailedMatchIncludingEventsLineupsAndStatistics() {
        ProviderMatchDetails details = details(fixture(10, "2H", 67, 2, 1));
        LiveMatchService service = new LiveMatchService(stub(List.of(), details), properties, clock);

        MatchDetailsResponse response = service.getMatchDetails(10);

        assertThat(response.fixtureId()).isEqualTo(10);
        assertThat(response.status()).isEqualTo("LIVE");
        assertThat(response.extraMinutes()).isEqualTo(2);
        assertThat(response.refreshIntervalSeconds()).isEqualTo(60);
        assertThat(response.events()).extracting(MatchEventResponse::playerName).containsExactly("Saka");
        assertThat(response.lineups()).hasSize(1);
        assertThat(response.lineups().getFirst().formation()).isEqualTo("4-3-3");
        assertThat(response.statistics().getFirst().statistics().getFirst().type()).isEqualTo("Shots on Goal");
        assertThat(response.venue().name()).isEqualTo("Emirates Stadium");
    }

    @Test
    void missingMatchIsNotFound() {
        LiveMatchService service = new LiveMatchService(stub(List.of(), null), properties, clock);

        assertThatThrownBy(() -> service.getMatchDetails(99))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.MATCH_NOT_FOUND);
    }

    private static FootballDataProvider stub(List<ProviderFixture> live, ProviderMatchDetails details) {
        return new FootballDataProvider() {
            @Override
            public SeasonSchedule loadPremierLeagueSeason(int season) {
                return new SeasonSchedule(List.of(), List.of());
            }

            @Override
            public List<ProviderFixture> loadLivePremierLeagueMatches() {
                return live;
            }

            @Override
            public ProviderMatchDetails loadPremierLeagueMatchDetails(long fixtureId) {
                if (details == null || details.fixture().id() != fixtureId) {
                    throw ApiException.matchNotFound(fixtureId);
                }
                return details;
            }
        };
    }

    private static ProviderFixture fixture(long id, String status, int elapsed, int home, int away) {
        return new ProviderFixture(
                id,
                "Regular Season - 4",
                Instant.parse("2026-09-07T14:00:00Z"),
                status,
                "Second Half",
                elapsed,
                42L,
                "Arsenal",
                "https://example.com/arsenal.png",
                49L,
                "Chelsea",
                "https://example.com/chelsea.png",
                home,
                away
        );
    }

    private static ProviderMatchDetails details(ProviderFixture fixture) {
        return new ProviderMatchDetails(
                fixture,
                39,
                "Premier League",
                2,
                "M. Oliver",
                "Emirates Stadium",
                "London",
                new ProviderMatchDetails.ScoreBreakdown(1, 0, null, null, null, null, null, null),
                List.of(new ProviderMatchDetails.Event(
                        23, null, 42L, "Arsenal", 100L, "Saka", 101L, "Odegaard",
                        "Goal", "Normal Goal", null
                )),
                List.of(new ProviderMatchDetails.Lineup(
                        42L, "Arsenal", "https://example.com/arsenal.png", "4-3-3", "Mikel Arteta",
                        List.of(new ProviderMatchDetails.LineupPlayer(100L, "Saka", 7, "F", "4:3")),
                        List.of()
                )),
                List.of(new ProviderMatchDetails.TeamStatistics(
                        42L, "Arsenal",
                        List.of(new ProviderMatchDetails.Stat("Shots on Goal", "6"))
                ))
        );
    }
}
