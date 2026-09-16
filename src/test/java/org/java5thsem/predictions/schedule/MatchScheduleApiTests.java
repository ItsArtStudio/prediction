package org.java5thsem.predictions.schedule;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class MatchScheduleApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballDataProvider footballDataProvider;

    @Test
    void returnsScheduleForDateRange() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(2026)).thenReturn(new SeasonSchedule(
                List.of(),
                List.of(fixture(10, Instant.parse("2026-09-12T14:00:00Z"), "NS"))
        ));

        mockMvc.perform(get("/api/leagues/premier-league/schedule")
                        .param("from", "2026-09-12")
                        .param("to", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.league").value("Premier League"))
                .andExpect(jsonPath("$.leagueCode").value("EPL"))
                .andExpect(jsonPath("$.upcomingOnly").value(false))
                .andExpect(jsonPath("$.from").value("2026-09-12"))
                .andExpect(jsonPath("$.to").value("2026-09-14"))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].homeTeam.name").value("Arsenal"))
                .andExpect(jsonPath("$.matches[0].awayTeam.name").value("Chelsea"))
                .andExpect(jsonPath("$.matches[0].league.name").value("Premier League"))
                .andExpect(jsonPath("$.matches[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.matches[0].kickoffAt").exists());
    }

    @Test
    void invalidRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/schedule")
                        .param("from", "2026-09-14")
                        .param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void invalidDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/schedule").param("date", "07-09-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void providerFailureIsMapped() throws Exception {
        when(footballDataProvider.loadPremierLeagueSeason(anyInt()))
                .thenThrow(ApiException.providerUnavailable("API-Football is down"));

        mockMvc.perform(get("/api/leagues/premier-league/schedule"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    private static ProviderFixture fixture(long id, Instant kickoff, String status) {
        return new ProviderFixture(
                id,
                "Regular Season - 4",
                kickoff,
                status,
                "Not Started",
                null,
                42L,
                "Arsenal",
                "https://example.com/arsenal.png",
                49L,
                "Chelsea",
                "https://example.com/chelsea.png",
                null,
                null
        );
    }
}
