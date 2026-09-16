package org.java5thsem.predictions.provider;

import java.util.List;

public record ProviderMatchDetails(
        ProviderFixture fixture,
        int leagueId,
        String leagueName,
        Integer extraMinutes,
        String referee,
        String venueName,
        String venueCity,
        ScoreBreakdown scoreBreakdown,
        List<Event> events,
        List<Lineup> lineups,
        List<TeamStatistics> statistics
) {
    public record Event(
            Integer elapsedMinutes,
            Integer extraMinutes,
            long teamId,
            String teamName,
            Long playerId,
            String playerName,
            Long assistId,
            String assistName,
            String type,
            String detail,
            String comments
    ) {
    }

    public record LineupPlayer(
            long id,
            String name,
            Integer number,
            String position,
            String grid
    ) {
    }

    public record Lineup(
            long teamId,
            String teamName,
            String teamLogo,
            String formation,
            String coachName,
            List<LineupPlayer> startXi,
            List<LineupPlayer> substitutes
    ) {
    }

    public record Stat(String type, String value) {
    }

    public record TeamStatistics(
            long teamId,
            String teamName,
            List<Stat> statistics
    ) {
    }

    public record ScoreBreakdown(
            Integer halftimeHome,
            Integer halftimeAway,
            Integer fulltimeHome,
            Integer fulltimeAway,
            Integer extraTimeHome,
            Integer extraTimeAway,
            Integer penaltyHome,
            Integer penaltyAway
    ) {
    }
}
