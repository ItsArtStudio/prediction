package org.java5thsem.predictions.provider.apifootball;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.matchweek.MatchStatusMapper;
import org.java5thsem.predictions.provider.FootballDataProvider;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.java5thsem.predictions.provider.ProviderStandings;
import org.java5thsem.predictions.provider.SeasonSchedule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ApiFootballClient implements FootballDataProvider {

    private final RestClient restClient;
    private final ApiFootballProperties properties;
    private final Clock clock;
    private final ApiFootballMapper mapper = new ApiFootballMapper();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ConcurrentHashMap<Integer, CachedSeason> seasonCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CachedStandings> standingsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CachedDetails> detailsCache = new ConcurrentHashMap<>();
    private volatile CachedLive liveCache;

    public ApiFootballClient(
            @Qualifier("apiFootballRestClient") RestClient restClient,
            ApiFootballProperties properties,
            Clock clock
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SeasonSchedule loadPremierLeagueSeason(int season) {
        ensureConfigured();
        CachedSeason cached = seasonCache.get(season);
        Instant now = Instant.now(clock);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.schedule();
        }
        SeasonSchedule schedule = fetchSeason(season);
        seasonCache.put(season, new CachedSeason(schedule, now.plus(properties.cacheTtl())));
        return schedule;
    }

    @Override
    public List<ProviderFixture> loadLivePremierLeagueMatches() {
        ensureConfigured();
        Instant now = Instant.now(clock);
        CachedLive cached = liveCache;
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.matches();
        }
        List<ProviderFixture> matches = mapper.fixtures(getJson("/fixtures", Map.of(
                "live", String.valueOf(properties.premierLeagueId()),
                "timezone", "UTC"
        ))).stream()
                .filter(fixture -> MatchStatusMapper.isLive(fixture.statusShort()))
                .toList();
        liveCache = new CachedLive(matches, now.plus(liveTtl()));
        return matches;
    }

    @Override
    public ProviderMatchDetails loadPremierLeagueMatchDetails(long fixtureId) {
        ensureConfigured();
        Instant now = Instant.now(clock);
        CachedDetails cached = detailsCache.get(fixtureId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.details();
        }
        ProviderMatchDetails details = fetchMatchDetails(fixtureId);
        Duration ttl = MatchStatusMapper.isLive(details.fixture().statusShort()) ? liveTtl() : properties.cacheTtl();
        detailsCache.put(fixtureId, new CachedDetails(details, now.plus(ttl)));
        return details;
    }

    @Override
    public ProviderStandings loadPremierLeagueStandings(int season) {
        ensureConfigured();
        CachedStandings cached = standingsCache.get(season);
        Instant now = Instant.now(clock);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.standings();
        }
        ProviderStandings standings = fetchStandings(season);
        standingsCache.put(season, new CachedStandings(standings, now.plus(properties.cacheTtl())));
        return standings;
    }

    private SeasonSchedule fetchSeason(int season) {
        int leagueId = properties.premierLeagueId();
        List<String> rounds = mapper.rounds(getJson("/fixtures/rounds", Map.of(
                "league", leagueId,
                "season", season,
                "timezone", "UTC"
        )));
        List<ProviderFixture> fixtures = mapper.fixtures(getJson("/fixtures", Map.of(
                "league", leagueId,
                "season", season,
                "timezone", "UTC"
        )));
        if (rounds.isEmpty() && fixtures.isEmpty()) {
            throw ApiException.providerUnavailable(
                    "API-Football returned no Premier League matchweeks for season " + season);
        }
        return new SeasonSchedule(rounds, fixtures);
    }

    private ProviderStandings fetchStandings(int season) {
        ProviderStandings standings = mapper.standings(getJson("/standings", Map.of(
                "league", properties.premierLeagueId(),
                "season", season
        )));
        if (standings == null || standings.leagueId() != properties.premierLeagueId()) {
            throw ApiException.providerUnavailable(
                    "API-Football returned no Premier League standings for season " + season);
        }
        return standings;
    }

    private ProviderMatchDetails fetchMatchDetails(long fixtureId) {
        JsonNode fixtureRoot = getJson("/fixtures", Map.of(
                "id", fixtureId,
                "timezone", "UTC"
        ));
        List<ProviderMatchDetails.Event> events = optionalData(
                () -> mapper.events(getJson("/fixtures/events", Map.of("fixture", fixtureId))),
                List.of()
        );
        List<ProviderMatchDetails.Lineup> lineups = optionalData(
                () -> mapper.lineups(getJson("/fixtures/lineups", Map.of("fixture", fixtureId))),
                List.of()
        );
        List<ProviderMatchDetails.TeamStatistics> statistics = optionalData(
                () -> mapper.statistics(getJson("/fixtures/statistics", Map.of("fixture", fixtureId))),
                List.of()
        );
        ProviderMatchDetails details = mapper.matchDetails(fixtureRoot, events, lineups, statistics);
        if (details == null) {
            throw ApiException.matchNotFound(fixtureId);
        }
        if (details.leagueId() != properties.premierLeagueId()) {
            throw ApiException.matchNotFound(fixtureId);
        }
        return details;
    }

    private <T> T optionalData(Supplier<T> loader, T fallback) {
        try {
            return loader.get();
        } catch (ApiException exception) {
            if (exception.getCode() == ErrorCode.PROVIDER_UNAVAILABLE) {
                return fallback;
            }
            throw exception;
        }
    }

    private void ensureConfigured() {
        if (!properties.hasApiKey()) {
            throw ApiException.providerNotConfigured();
        }
    }

    private Duration liveTtl() {
        Duration ttl = properties.liveCacheTtl();
        return ttl == null ? Duration.ofMinutes(1) : ttl;
    }

    private JsonNode getJson(String path, Map<String, ?> query) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        query.forEach((key, value) -> {
                            if (value != null) {
                                uriBuilder.queryParam(key, value);
                            }
                        });
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 429) {
                            throw ApiException.providerRateLimited();
                        }
                        if (status == 401 || status == 403) {
                            throw ApiException.providerUnavailable(
                                    "API-Football rejected the request. Check the API_FOOTBALL_KEY");
                        }
                        throw ApiException.providerUnavailable(
                                "API-Football request failed with HTTP " + status);
                    })
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw ApiException.providerUnavailable("API-Football returned an empty response");
            }
            return jsonMapper.readTree(body);
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw ApiException.providerUnavailable(
                    "API-Football request failed with HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw ApiException.providerUnavailable("Could not reach API-Football: " + exception.getMessage());
        } catch (RuntimeException exception) {
            throw ApiException.providerUnavailable("Could not parse API-Football response");
        }
    }

    private record CachedSeason(SeasonSchedule schedule, Instant expiresAt) {
    }

    private record CachedLive(List<ProviderFixture> matches, Instant expiresAt) {
    }

    private record CachedDetails(ProviderMatchDetails details, Instant expiresAt) {
    }

    private record CachedStandings(ProviderStandings standings, Instant expiresAt) {
    }
}
