package org.java5thsem.predictions.live;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveMatchApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballDataProvider footballDataProvider;

    @Test
    void listsLivePremierLeagueMatches() throws Exception {
        when(footballDataProvider.loadLivePremierLeagueMatches()).thenReturn(List.of(liveFixture()));

        mockMvc.perform(get("/api/leagues/premier-league/matches/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leagueCode").value("EPL"))
                .andExpect(jsonPath("$.refreshIntervalSeconds").value(60))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].status").value("LIVE"))
                .andExpect(jsonPath("$.matches[0].elapsedMinutes").value(67))
                .andExpect(jsonPath("$.matches[0].homeTeam.name").value("Arsenal"))
                .andExpect(jsonPath("$.matches[0].score.home").value(2));
    }

    @Test
    void returnsMatchDetails() throws Exception {
        when(footballDataProvider.loadPremierLeagueMatchDetails(10L)).thenReturn(details());

        mockMvc.perform(get("/api/leagues/premier-league/matches/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixtureId").value(10))
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.events[0].playerName").value("Saka"))
                .andExpect(jsonPath("$.lineups[0].formation").value("4-3-3"))
                .andExpect(jsonPath("$.statistics[0].statistics[0].type").value("Shots on Goal"))
                .andExpect(jsonPath("$.venue.name").value("Emirates Stadium"));
    }

    @Test
    void unknownMatchReturnsNotFound() throws Exception {
        when(footballDataProvider.loadPremierLeagueMatchDetails(anyLong()))
                .thenThrow(ApiException.matchNotFound(99L));

        mockMvc.perform(get("/api/leagues/premier-league/matches/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void providerFailureIsMapped() throws Exception {
        when(footballDataProvider.loadLivePremierLeagueMatches())
                .thenThrow(ApiException.providerUnavailable("API-Football is down"));

        mockMvc.perform(get("/api/leagues/premier-league/matches/live"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    private static ProviderFixture liveFixture() {
        return new ProviderFixture(
                10L,
                "Regular Season - 4",
                Instant.parse("2026-09-07T14:00:00Z"),
                "2H",
                "Second Half",
                67,
                42L,
                "Arsenal",
                "https://example.com/arsenal.png",
                49L,
                "Chelsea",
                "https://example.com/chelsea.png",
                2,
                1
        );
    }

    private static ProviderMatchDetails details() {
        ProviderFixture fixture = liveFixture();
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
