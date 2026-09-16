package org.java5thsem.predictions.matchweek;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchweekServiceTests {

    private final ApiFootballProperties properties = new ApiFootballProperties(
            "https://v3.football.api-sports.io",
            "test-key",
            39,
            2026,
            Duration.ofMinutes(2),
            Duration.ofMinutes(1)
    );

    @Test
    void listsMatchweeksWithDatesStatusAndCurrentRound() {
        Instant first = Instant.parse("2026-08-15T14:00:00Z");
        Instant last = Instant.parse("2026-08-16T16:30:00Z");
        Instant upcoming = Instant.parse("2026-08-22T14:00:00Z");
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of("Regular Season - 1", "Regular Season - 2"),
                List.of(
                        fixture(1, "Regular Season - 1", first, "FT", 2, 1),
                        fixture(2, "Regular Season - 1", last, "FT", 0, 0),
                        fixture(3, "Regular Season - 2", upcoming, "NS", null, null)
                )
        );
        MatchweekService service = new MatchweekService(provider, properties);

        MatchweekListResponse response = service.listMatchweeks(null);

        assertThat(response.league()).isEqualTo("Premier League");
        assertThat(response.leagueCode()).isEqualTo("EPL");
        assertThat(response.season()).isEqualTo(2026);
        assertThat(response.currentMatchweek()).isEqualTo(2);
        assertThat(response.matchweeks()).hasSize(2);
        assertThat(response.matchweeks().getFirst().number()).isEqualTo(1);
        assertThat(response.matchweeks().getFirst().name()).isEqualTo("Regular Season - 1");
        assertThat(response.matchweeks().getFirst().status()).isEqualTo("FINISHED");
        assertThat(response.matchweeks().getFirst().startAt()).isEqualTo(first);
        assertThat(response.matchweeks().getFirst().endAt()).isEqualTo(last);
        assertThat(response.matchweeks().get(1).status()).isEqualTo("SCHEDULED");
    }

    @Test
    void returnsMatchesForASpecificMatchweek() {
        Instant kickoff = Instant.parse("2026-08-15T14:00:00Z");
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of("Regular Season - 1", "Regular Season - 2"),
                List.of(
                        fixture(10, "Regular Season - 1", kickoff, "FT", 2, 1),
                        fixture(11, "Regular Season - 2", kickoff.plusSeconds(86400), "NS", null, null)
                )
        );
        MatchweekService service = new MatchweekService(provider, properties);

        MatchweekMatchesResponse response = service.listMatches(1, 2026);

        assertThat(response.matchweek().number()).isEqualTo(1);
        assertThat(response.matches()).hasSize(1);
        MatchweekMatchResponse match = response.matches().getFirst();
        assertThat(match.fixtureId()).isEqualTo(10);
        assertThat(match.homeTeam().name()).isEqualTo("Arsenal");
        assertThat(match.awayTeam().name()).isEqualTo("Chelsea");
        assertThat(match.kickoffAt()).isEqualTo(kickoff);
        assertThat(match.score().home()).isEqualTo(2);
        assertThat(match.score().away()).isEqualTo(1);
        assertThat(match.status()).isEqualTo("FINISHED");
        assertThat(match.statusCode()).isEqualTo("FT");
    }

    @Test
    void unknownMatchweekIsNotFound() {
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of("Regular Season - 1"),
                List.of()
        );
        MatchweekService service = new MatchweekService(provider, properties);

        assertThatThrownBy(() -> service.listMatches(12, 2026))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.MATCHWEEK_NOT_FOUND);
    }

    @Test
    void liveMatchMakesMatchweekLive() {
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of("Regular Season - 5"),
                List.of(fixture(20, "Regular Season - 5", Instant.parse("2026-09-05T14:00:00Z"), "2H", 1, 0))
        );
        MatchweekService service = new MatchweekService(provider, properties);

        assertThat(service.listMatchweeks(2026).matchweeks().getFirst().status()).isEqualTo("LIVE");
        assertThat(service.listMatchweeks(2026).currentMatchweek()).isEqualTo(5);
    }

    private static ProviderFixture fixture(
            long id,
            String round,
            Instant kickoff,
            String status,
            Integer homeGoals,
            Integer awayGoals
    ) {
        return new ProviderFixture(
                id,
                round,
                kickoff,
                status,
                status,
                "FT".equals(status) ? 90 : null,
                42L,
                "Arsenal",
                "https://example.com/arsenal.png",
                49L,
                "Chelsea",
                "https://example.com/chelsea.png",
                homeGoals,
                awayGoals
        );
    }
}
