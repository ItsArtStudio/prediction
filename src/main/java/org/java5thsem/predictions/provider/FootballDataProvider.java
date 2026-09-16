package org.java5thsem.predictions.provider;

import java.util.List;

public interface FootballDataProvider {

    SeasonSchedule loadPremierLeagueSeason(int season);

    default List<ProviderFixture> loadLivePremierLeagueMatches() {
        throw new UnsupportedOperationException("Live matches are not supported by this provider");
    }

    default ProviderMatchDetails loadPremierLeagueMatchDetails(long fixtureId) {
        throw new UnsupportedOperationException("Match details are not supported by this provider");
    }

    default ProviderStandings loadPremierLeagueStandings(int season) {
        throw new UnsupportedOperationException("Standings are not supported by this provider");
    }
}
