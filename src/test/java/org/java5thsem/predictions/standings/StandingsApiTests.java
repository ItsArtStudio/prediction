package org.java5thsem.predictions.standings;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderStanding;
import org.java5thsem.predictions.provider.ProviderStandings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StandingsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballDataProvider footballDataProvider;

    @Test
    void returnsPremierLeagueStandings() throws Exception {
        Instant updatedAt = Instant.parse("2026-09-07T14:00:00Z");
        when(footballDataProvider.loadPremierLeagueStandings(2026)).thenReturn(new ProviderStandings(
                39,
                "Premier League",
                2026,
                updatedAt,
                List.of(
                        new ProviderStanding(
                                1, 42L, "Arsenal", "https://example.com/arsenal.png",
                                13, 5, 4, 1, 0, 12, 3, 9, updatedAt
                        ),
                        new ProviderStanding(
                                2, 49L, "Chelsea", "https://example.com/chelsea.png",
                                10, 5, 3, 1, 1, 8, 5, 3, updatedAt
                        )
                )
        ));

        mockMvc.perform(get("/api/leagues/premier-league/standings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.league").value("Premier League"))
                .andExpect(jsonPath("$.leagueCode").value("EPL"))
                .andExpect(jsonPath("$.season").value(2026))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-07T14:00:00Z"))
                .andExpect(jsonPath("$.standings.length()").value(2))
                .andExpect(jsonPath("$.standings[0].position").value(1))
                .andExpect(jsonPath("$.standings[0].team.name").value("Arsenal"))
                .andExpect(jsonPath("$.standings[0].points").value(13))
                .andExpect(jsonPath("$.standings[0].played").value(5))
                .andExpect(jsonPath("$.standings[0].won").value(4))
                .andExpect(jsonPath("$.standings[0].drawn").value(1))
                .andExpect(jsonPath("$.standings[0].lost").value(0))
                .andExpect(jsonPath("$.standings[0].goalsFor").value(12))
                .andExpect(jsonPath("$.standings[0].goalsAgainst").value(3))
                .andExpect(jsonPath("$.standings[0].goalDifference").value(9));
    }

    @Test
    void providerFailureIsMappedToBadGateway() throws Exception {
        when(footballDataProvider.loadPremierLeagueStandings(anyInt()))
                .thenThrow(ApiException.providerUnavailable("API-Football returned an error: rate limit"));

        mockMvc.perform(get("/api/leagues/premier-league/standings"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    @Test
    void missingProviderKeyIsServiceUnavailable() throws Exception {
        when(footballDataProvider.loadPremierLeagueStandings(anyInt()))
                .thenThrow(ApiException.providerNotConfigured());

        mockMvc.perform(get("/api/leagues/premier-league/standings"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROVIDER_NOT_CONFIGURED"));
    }
}
