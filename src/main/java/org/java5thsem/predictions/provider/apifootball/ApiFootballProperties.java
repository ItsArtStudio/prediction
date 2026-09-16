package org.java5thsem.predictions.provider.apifootball;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sports.api-football")
public record ApiFootballProperties(
        String baseUrl,
        String apiKey,
        int premierLeagueId,
        int season,
        Duration cacheTtl,
        Duration liveCacheTtl
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
