package org.java5thsem.predictions.live;

public record MatchEventResponse(
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
