package org.java5thsem.predictions.schedule;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchScheduleServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-07T12:00:00Z");
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
    void defaultScheduleReturnsUpcomingMatchesOnly() {
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of(),
                List.of(
                        fixture(1, Instant.parse("2026-09-01T14:00:00Z"), "FT", 2, 1),
                        fixture(2, Instant.parse("2026-09-07T11:00:00Z"), "NS", null, null),
                        fixture(3, Instant.parse("2026-09-12T14:00:00Z"), "NS", null, null),
                        fixture(4, Instant.parse("2026-09-12T16:30:00Z"), "PST", null, null),
                        fixture(5, Instant.parse("2026-09-07T15:00:00Z"), "2H", 1, 0)
                )
        );
        MatchScheduleService service = new MatchScheduleService(provider, properties, clock);

        MatchScheduleResponse response = service.getSchedule(null, null, null, null);

        assertThat(response.upcomingOnly()).isTrue();
        assertThat(response.matches()).extracting(ScheduledMatchResponse::fixtureId)
                .containsExactly(3L, 4L);
        assertThat(response.matches().getFirst().league().name()).isEqualTo("Premier League");
        assertThat(response.matches().getFirst().league().code()).isEqualTo("EPL");
        assertThat(response.matches().getFirst().homeTeam().name()).isEqualTo("Arsenal");
        assertThat(response.matches().getFirst().status()).isEqualTo("SCHEDULED");
    }

    @Test
    void singleDateIncludesFinishedAndUpcomingOnThatDay() {
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of(),
                List.of(
                        fixture(1, Instant.parse("2026-09-07T11:00:00Z"), "FT", 1, 0),
                        fixture(2, Instant.parse("2026-09-07T19:00:00Z"), "NS", null, null),
                        fixture(3, Instant.parse("2026-09-08T14:00:00Z"), "NS", null, null)
                )
        );
        MatchScheduleService service = new MatchScheduleService(provider, properties, clock);

        MatchScheduleResponse response = service.getSchedule(2026, LocalDate.of(2026, 9, 7), null, null);

        assertThat(response.upcomingOnly()).isFalse();
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(response.matches()).extracting(ScheduledMatchResponse::fixtureId)
                .containsExactly(1L, 2L);
    }

    @Test
    void dateRangeFiltersInclusive() {
        FootballDataProvider provider = season -> new SeasonSchedule(
                List.of(),
                List.of(
                        fixture(1, Instant.parse("2026-09-10T14:00:00Z"), "NS", null, null),
                        fixture(2, Instant.parse("2026-09-12T14:00:00Z"), "NS", null, null),
                        fixture(3, Instant.parse("2026-09-14T14:00:00Z"), "NS", null, null)
                )
        );
        MatchScheduleService service = new MatchScheduleService(provider, properties, clock);

        MatchScheduleResponse response = service.getSchedule(
                2026,
                null,
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 14)
        );

        assertThat(response.from()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(response.to()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.matches()).extracting(ScheduledMatchResponse::fixtureId)
                .containsExactly(2L, 3L);
    }

    @Test
    void rejectsDateCombinedWithRange() {
        MatchScheduleService service = new MatchScheduleService(season -> new SeasonSchedule(List.of(), List.of()), properties, clock);

        assertThatThrownBy(() -> service.getSchedule(
                2026,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 1),
                null
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void rejectsInvertedRange() {
        MatchScheduleService service = new MatchScheduleService(season -> new SeasonSchedule(List.of(), List.of()), properties, clock);

        assertThatThrownBy(() -> service.getSchedule(
                2026,
                null,
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 1)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    private static ProviderFixture fixture(
            long id,
            Instant kickoff,
            String status,
            Integer homeGoals,
            Integer awayGoals
    ) {
        return new ProviderFixture(
                id,
                "Regular Season - 4",
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
