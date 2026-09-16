package org.java5thsem.predictions.matchweek;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.SeasonSchedule;
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
class MatchweekApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballDataProvider footballDataProvider;

    @Test
    void listsPremierLeagueMatchweeks() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(2026)).thenReturn(schedule());

        mockMvc.perform(get("/api/leagues/premier-league/matchweeks").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.league").value("Premier League"))
                .andExpect(jsonPath("$.leagueCode").value("EPL"))
                .andExpect(jsonPath("$.season").value(2026))
                .andExpect(jsonPath("$.matchweeks.length()").value(2))
                .andExpect(jsonPath("$.matchweeks[0].number").value(1))
                .andExpect(jsonPath("$.matchweeks[0].name").value("Regular Season - 1"))
                .andExpect(jsonPath("$.matchweeks[0].status").value("FINISHED"));
    }

    @Test
    void listsMatchesForMatchweek() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(2026)).thenReturn(schedule());

        mockMvc.perform(get("/api/leagues/premier-league/matchweeks/1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchweek.number").value(1))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].homeTeam.name").value("Arsenal"))
                .andExpect(jsonPath("$.matches[0].awayTeam.name").value("Chelsea"))
                .andExpect(jsonPath("$.matches[0].score.home").value(2))
                .andExpect(jsonPath("$.matches[0].score.away").value(1))
                .andExpect(jsonPath("$.matches[0].status").value("FINISHED"))
                .andExpect(jsonPath("$.matches[0].kickoffAt").exists());
    }

    @Test
    void unknownMatchweekReturnsNotFound() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(anyInt())).thenReturn(schedule());

        mockMvc.perform(get("/api/leagues/premier-league/matchweeks/99/matches"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCHWEEK_NOT_FOUND"));
    }

    @Test
    void providerFailureIsMappedToBadGateway() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(anyInt()))
                .thenThrow(ApiException.providerUnavailable("API-Football returned an error: rate limit"));

        mockMvc.perform(get("/api/leagues/premier-league/matchweeks"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    @Test
    void missingProviderKeyIsServiceUnavailable() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(anyInt()))
                .thenThrow(ApiException.providerNotConfigured());

        mockMvc.perform(get("/api/leagues/premier-league/matchweeks"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROVIDER_NOT_CONFIGURED"));
    }

    private static SeasonSchedule schedule() {
        Instant kickoff = Instant.parse("2026-08-15T14:00:00Z");
        return new SeasonSchedule(
                List.of("Regular Season - 1", "Regular Season - 2"),
                List.of(
                        new ProviderFixture(
                                10L,
                                "Regular Season - 1",
                                kickoff,
                                "FT",
                                "Match Finished",
                                90,
                                42L,
                                "Arsenal",
                                "https://example.com/arsenal.png",
                                49L,
                                "Chelsea",
                                "https://example.com/chelsea.png",
                                2,
                                1
                        ),
                        new ProviderFixture(
                                11L,
                                "Regular Season - 2",
                                kickoff.plusSeconds(86400),
                                "NS",
                                "Not Started",
                                null,
                                40L,
                                "Liverpool",
                                "https://example.com/liverpool.png",
                                50L,
                                "Manchester City",
                                "https://example.com/city.png",
                                null,
                                null
                        )
                )
        );
    }
}
