package org.java5thsem.predictions.live;

import java.util.List;

public record TeamStatisticsResponse(
        long teamId,
        String teamName,
        List<StatisticResponse> statistics
) {
    public record StatisticResponse(String type, String value) {
    }
}
