package org.java5thsem.predictions.provider.apifootball;

import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiFootballMatchDetailsMapperTests {

    private final ApiFootballMapper mapper = new ApiFootballMapper();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsEventsLineupsAndStatistics() {
        var events = mapper.events(jsonMapper.readTree("""
                {"errors":[],"response":[{
                  "time":{"elapsed":23,"extra":null},
                  "team":{"id":42,"name":"Arsenal"},
                  "player":{"id":100,"name":"Saka"},
                  "assist":{"id":101,"name":"Odegaard"},
                  "type":"Goal","detail":"Normal Goal","comments":null
                }]}
                """));
        var lineups = mapper.lineups(jsonMapper.readTree("""
                {"errors":[],"response":[{
                  "team":{"id":42,"name":"Arsenal","logo":"https://example.com/arsenal.png"},
                  "formation":"4-3-3",
                  "coach":{"name":"Mikel Arteta"},
                  "startXI":[{"player":{"id":100,"name":"Saka","number":7,"pos":"F","grid":"4:3"}}],
                  "substitutes":[{"player":{"id":102,"name":"Trossard","number":19,"pos":"M","grid":null}}]
                }]}
                """));
        var statistics = mapper.statistics(jsonMapper.readTree("""
                {"errors":[],"response":[{
                  "team":{"id":42,"name":"Arsenal"},
                  "statistics":[
                    {"type":"Shots on Goal","value":6},
                    {"type":"Ball Possession","value":"62%"}
                  ]
                }]}
                """));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().playerName()).isEqualTo("Saka");
        assertThat(events.getFirst().type()).isEqualTo("Goal");
        assertThat(lineups.getFirst().formation()).isEqualTo("4-3-3");
        assertThat(lineups.getFirst().startXi()).hasSize(1);
        assertThat(statistics.getFirst().statistics().getFirst().value()).isEqualTo("6");
        assertThat(statistics.getFirst().statistics().get(1).value()).isEqualTo("62%");
    }

    @Test
    void mapsMatchDetailsFromFixturePayload() {
        var root = jsonMapper.readTree("""
                {"errors":[],"response":[{
                  "fixture":{
                    "id":1208001,
                    "referee":"M. Oliver",
                    "date":"2026-09-07T14:00:00+00:00",
                    "venue":{"name":"Emirates Stadium","city":"London"},
                    "status":{"long":"Second Half","short":"2H","elapsed":67,"extra":2}
                  },
                  "league":{"id":39,"name":"Premier League","round":"Regular Season - 4"},
                  "teams":{
                    "home":{"id":42,"name":"Arsenal","logo":"https://example.com/arsenal.png"},
                    "away":{"id":49,"name":"Chelsea","logo":"https://example.com/chelsea.png"}
                  },
                  "goals":{"home":2,"away":1},
                  "score":{"halftime":{"home":1,"away":0},"fulltime":{"home":null,"away":null}}
                }]}
                """);

        ProviderMatchDetails details = mapper.matchDetails(root, java.util.List.of(), java.util.List.of(), java.util.List.of());
        assertThat(details).isNotNull();
        assertThat(details.leagueId()).isEqualTo(39);
        assertThat(details.extraMinutes()).isEqualTo(2);
        assertThat(details.referee()).isEqualTo("M. Oliver");
        assertThat(details.venueName()).isEqualTo("Emirates Stadium");
        assertThat(details.fixture().elapsedMinutes()).isEqualTo(67);
        assertThat(details.scoreBreakdown().halftimeHome()).isEqualTo(1);
    }
}
