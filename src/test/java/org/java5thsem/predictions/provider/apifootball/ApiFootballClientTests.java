package org.java5thsem.predictions.provider.apifootball;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ApiFootballClientTests {

    @Test
    void missingApiKeyFailsFast() {
        ApiFootballProperties properties = new ApiFootballProperties(
                "https://v3.football.api-sports.io",
                "",
                39,
                2026,
                Duration.ofMinutes(2),
                Duration.ofMinutes(1)
        );
        ApiFootballClient client = new ApiFootballClient(mock(RestClient.class), properties, Clock.systemUTC());

        assertThatThrownBy(() -> client.loadPremierLeagueSeason(2026))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.PROVIDER_NOT_CONFIGURED);
        assertThatThrownBy(() -> client.loadPremierLeagueStandings(2026))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.PROVIDER_NOT_CONFIGURED);
    }
}
