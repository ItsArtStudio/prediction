package org.java5thsem.predictions.live;

import java.util.List;

public record LineupResponse(
        long teamId,
        String teamName,
        String teamLogo,
        String formation,
        String coachName,
        List<LineupPlayerResponse> startXi,
        List<LineupPlayerResponse> substitutes
) {
}
