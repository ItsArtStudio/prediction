package org.java5thsem.predictions.standings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leagues/premier-league")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping("/standings")
    public LeagueStandingsResponse getStandings(
            @RequestParam(required = false)
            @Min(2000)
            @Max(2100)
            Integer season
    ) {
        return standingsService.getStandings(season);
    }
}
