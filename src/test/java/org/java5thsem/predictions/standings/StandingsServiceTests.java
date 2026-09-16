package org.java5thsem.predictions.standings;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderStanding;
import org.java5thsem.predictions.provider.ProviderStandings;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandingsServiceTests {

    private final ApiFootballProperties properties = new ApiFootballProperties(
            "https://v3.football.api-sports.io",
            "test-key",
            39,
            2026,
            Duration.ofMinutes(2),
            Duration.ofMinutes(1)
    );

    @Test
    void returnsFullPremierLeagueTable() {
        Instant updatedAt = Instant.parse("2026-09-07T14:00:00Z");
        FootballDataProvider provider = stub(new ProviderStandings(
                39,
                "Premier League",
                2026,
                updatedAt,
                List.of(
                        standing(1, 50, "Manchester City", 13, 5, 4, 1, 0, 12, 3, 9, updatedAt),
                        standing(2, 42, "Arsenal", 12, 5, 4, 0, 1, 10, 4, 6, updatedAt)
                )
        ));
        StandingsService service = new StandingsService(provider, properties);

        LeagueStandingsResponse response = service.getStandings(null);

        assertThat(response.league()).isEqualTo("Premier League");
        assertThat(response.leagueCode()).isEqualTo("EPL");
        assertThat(response.season()).isEqualTo(2026);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.standings()).hasSize(2);
        StandingRowResponse city = response.standings().getFirst();
        assertThat(city.position()).isEqualTo(1);
        assertThat(city.team().name()).isEqualTo("Manchester City");
        assertThat(city.points()).isEqualTo(13);
        assertThat(city.played()).isEqualTo(5);
        assertThat(city.won()).isEqualTo(4);
        assertThat(city.drawn()).isEqualTo(1);
        assertThat(city.lost()).isEqualTo(0);
        assertThat(city.goalsFor()).isEqualTo(12);
        assertThat(city.goalsAgainst()).isEqualTo(3);
        assertThat(city.goalDifference()).isEqualTo(9);
    }

    @Test
    void usesRequestedSeason() {
        FootballDataProvider provider = stub(new ProviderStandings(
                39, "Premier League", 2025, Instant.parse("2026-05-25T00:00:00Z"), List.of()
        ));
        StandingsService service = new StandingsService(provider, properties);

        LeagueStandingsResponse response = service.getStandings(2025);

        assertThat(response.season()).isEqualTo(2025);
        assertThat(response.standings()).isEmpty();
    }

    @Test
    void providerErrorsPropagate() {
        FootballDataProvider provider = new FootballDataProvider() {
            @Override
            public SeasonSchedule loadPremierLeagueSeason(int season) {
                return new SeasonSchedule(List.of(), List.of());
            }

            @Override
            public ProviderStandings loadPremierLeagueStandings(int season) {
                throw ApiException.providerUnavailable("API-Football is down");
            }
        };
        StandingsService service = new StandingsService(provider, properties);

        assertThatThrownBy(() -> service.getStandings(2026))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
    }

    private static FootballDataProvider stub(ProviderStandings standings) {
        return new FootballDataProvider() {
            @Override
            public SeasonSchedule loadPremierLeagueSeason(int season) {
                return new SeasonSchedule(List.of(), List.of());
            }

            @Override
            public ProviderStandings loadPremierLeagueStandings(int season) {
                return standings;
            }
        };
    }

    private static ProviderStanding standing(
            int position,
            long teamId,
            String teamName,
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
        return new ProviderStanding(
                position,
                teamId,
                teamName,
                "https://example.com/" + teamId + ".png",
                points,
                played,
                won,
                drawn,
                lost,
                goalsFor,
                goalsAgainst,
                goalDifference,
                updatedAt
        );
    }
}
