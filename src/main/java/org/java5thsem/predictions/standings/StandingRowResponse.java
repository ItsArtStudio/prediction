package org.java5thsem.predictions.standings;

import org.java5thsem.predictions.matchweek.TeamResponse;

public record StandingRowResponse(
        int position,
        TeamResponse team,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {
}
