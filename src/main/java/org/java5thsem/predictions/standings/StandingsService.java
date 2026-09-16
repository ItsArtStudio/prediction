package org.java5thsem.predictions.standings;

import org.java5thsem.predictions.matchweek.TeamResponse;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderStanding;
import org.java5thsem.predictions.provider.ProviderStandings;
import org.java5thsem.predictions.provider.apifootball.ApiFootballProperties;
import org.springframework.stereotype.Service;

@Service
public class StandingsService {

    static final String LEAGUE_NAME = "Premier League";
    static final String LEAGUE_CODE = "EPL";

    private final FootballDataProvider footballDataProvider;
    private final ApiFootballProperties properties;

    public StandingsService(FootballDataProvider footballDataProvider, ApiFootballProperties properties) {
        this.footballDataProvider = footballDataProvider;
        this.properties = properties;
    }

    public LeagueStandingsResponse getStandings(Integer season) {
        int resolvedSeason = season == null ? properties.season() : season;
        ProviderStandings standings = footballDataProvider.loadPremierLeagueStandings(resolvedSeason);
        return new LeagueStandingsResponse(
                LEAGUE_NAME,
                LEAGUE_CODE,
                resolvedSeason,
                standings.updatedAt(),
                standings.table().stream().map(StandingsService::toRow).toList()
        );
    }

    private static StandingRowResponse toRow(ProviderStanding row) {
        return new StandingRowResponse(
                row.position(),
                new TeamResponse(row.teamId(), row.teamName(), row.teamLogo()),
                row.played(),
                row.won(),
                row.drawn(),
                row.lost(),
                row.goalsFor(),
                row.goalsAgainst(),
                row.goalDifference(),
                row.points()
        );
    }
}
