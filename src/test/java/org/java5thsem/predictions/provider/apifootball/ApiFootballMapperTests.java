package org.java5thsem.predictions.provider.apifootball;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiFootballMapperTests {

    private final ApiFootballMapper mapper = new ApiFootballMapper();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsRoundNames() {
        var root = jsonMapper.readTree("""
                {
                  "errors": [],
                  "response": ["Regular Season - 1", "Regular Season - 2"]
                }
                """);

        assertThat(mapper.rounds(root)).containsExactly("Regular Season - 1", "Regular Season - 2");
    }

    @Test
    void mapsFixturesIncludingScoreAndStatus() {
        var root = jsonMapper.readTree("""
                {
                  "errors": [],
                  "response": [{
                    "fixture": {
                      "id": 1208000,
                      "date": "2026-08-15T14:00:00+00:00",
                      "status": { "long": "Match Finished", "short": "FT", "elapsed": 90 }
                    },
                    "league": { "round": "Regular Season - 1" },
                    "teams": {
                      "home": { "id": 42, "name": "Arsenal", "logo": "https://example.com/arsenal.png" },
                      "away": { "id": 49, "name": "Chelsea", "logo": "https://example.com/chelsea.png" }
                    },
                    "goals": { "home": 2, "away": 1 }
                  }]
                }
                """);

        List<ProviderFixture> fixtures = mapper.fixtures(root);
        assertThat(fixtures).hasSize(1);
        ProviderFixture fixture = fixtures.getFirst();
        assertThat(fixture.id()).isEqualTo(1208000);
        assertThat(fixture.round()).isEqualTo("Regular Season - 1");
        assertThat(fixture.kickoffAt()).isEqualTo(Instant.parse("2026-08-15T14:00:00Z"));
        assertThat(fixture.statusShort()).isEqualTo("FT");
        assertThat(fixture.homeTeamName()).isEqualTo("Arsenal");
        assertThat(fixture.awayTeamName()).isEqualTo("Chelsea");
        assertThat(fixture.homeGoals()).isEqualTo(2);
        assertThat(fixture.awayGoals()).isEqualTo(1);
        assertThat(fixture.elapsedMinutes()).isEqualTo(90);
    }

    @Test
    void providerErrorObjectIsTranslated() {
        var root = jsonMapper.readTree("""
                { "errors": { "token": "Error/Missing application key" }, "response": [] }
                """);

        assertThatThrownBy(() -> mapper.rounds(root))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
    }

    @Test
    void mapsPremierLeagueStandings() {
        var root = jsonMapper.readTree("""
                {
                  "errors": [],
                  "response": [{
                    "league": {
                      "id": 39,
                      "name": "Premier League",
                      "season": 2026,
                      "standings": [[
                        {
                          "rank": 2,
                          "team": { "id": 42, "name": "Arsenal", "logo": "https://example.com/arsenal.png" },
                          "points": 12,
                          "goalsDiff": 6,
                          "all": {
                            "played": 5,
                            "win": 4,
                            "draw": 0,
                            "lose": 1,
                            "goals": { "for": 10, "against": 4 }
                          },
                          "update": "2026-09-07T14:00:00+00:00"
                        },
                        {
                          "rank": 1,
                          "team": { "id": 50, "name": "Manchester City", "logo": "https://example.com/city.png" },
                          "points": 13,
                          "goalsDiff": 9,
                          "all": {
                            "played": 5,
                            "win": 4,
                            "draw": 1,
                            "lose": 0,
                            "goals": { "for": 12, "against": 3 }
                          },
                          "update": "2026-09-07T14:00:00+00:00"
                        }
                      ]]
                    }
                  }]
                }
                """);

        var standings = mapper.standings(root);
        assertThat(standings.leagueId()).isEqualTo(39);
        assertThat(standings.season()).isEqualTo(2026);
        assertThat(standings.updatedAt()).isEqualTo(Instant.parse("2026-09-07T14:00:00Z"));
        assertThat(standings.table()).hasSize(2);
        var city = standings.table().getFirst();
        assertThat(city.position()).isEqualTo(1);
        assertThat(city.teamName()).isEqualTo("Manchester City");
        assertThat(city.points()).isEqualTo(13);
        assertThat(city.played()).isEqualTo(5);
        assertThat(city.won()).isEqualTo(4);
        assertThat(city.drawn()).isEqualTo(1);
        assertThat(city.lost()).isEqualTo(0);
        assertThat(city.goalsFor()).isEqualTo(12);
        assertThat(city.goalsAgainst()).isEqualTo(3);
        assertThat(city.goalDifference()).isEqualTo(9);
    }

    @Test
    void emptyStandingsResponseIsUnavailable() {
        var root = jsonMapper.readTree("""
                { "errors": [], "response": [] }
                """);

        assertThat(mapper.standings(root)).isNull();
    }
}
