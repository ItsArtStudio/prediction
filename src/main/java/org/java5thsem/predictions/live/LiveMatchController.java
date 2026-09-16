package org.java5thsem.predictions.live;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leagues/premier-league/matches")
public class LiveMatchController {

    private final LiveMatchService liveMatchService;

    public LiveMatchController(LiveMatchService liveMatchService) {
        this.liveMatchService = liveMatchService;
    }

    @GetMapping("/live")
    public LiveMatchesResponse listLiveMatches() {
        return liveMatchService.listLiveMatches();
    }

    @GetMapping("/{fixtureId}")
    public MatchDetailsResponse getMatchDetails(@PathVariable @Positive long fixtureId) {
        return liveMatchService.getMatchDetails(fixtureId);
    }
}
