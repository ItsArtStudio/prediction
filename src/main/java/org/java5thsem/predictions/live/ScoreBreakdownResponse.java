package org.java5thsem.predictions.live;

public record ScoreBreakdownResponse(
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
