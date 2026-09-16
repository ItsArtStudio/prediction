package org.java5thsem.predictions.matchweek;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leagues/premier-league")
public class MatchweekController {

    private final MatchweekService matchweekService;

    public MatchweekController(MatchweekService matchweekService) {
        this.matchweekService = matchweekService;
    }

    @GetMapping("/matchweeks")
    public MatchweekListResponse listMatchweeks(
            @RequestParam(required = false)
            @Min(2000)
            @Max(2100)
            Integer season
    ) {
        return matchweekService.listMatchweeks(season);
    }

    @GetMapping("/matchweeks/{matchweek}/matches")
    public MatchweekMatchesResponse listMatches(
            @PathVariable
            @Min(1)
            int matchweek,
            @RequestParam(required = false)
            @Min(2000)
            @Max(2100)
            Integer season
    ) {
        return matchweekService.listMatches(matchweek, season);
    }
}
