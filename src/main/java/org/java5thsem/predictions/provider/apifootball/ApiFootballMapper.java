package org.java5thsem.predictions.provider.apifootball;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.provider.ProviderFixture;
import org.java5thsem.predictions.provider.ProviderMatchDetails;
import org.java5thsem.predictions.provider.ProviderStanding;
import org.java5thsem.predictions.provider.ProviderStandings;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ApiFootballMapper {

    List<String> rounds(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            throw ApiException.providerUnavailable("API-Football returned an unexpected rounds payload");
        }
        List<String> rounds = new ArrayList<>();
        for (JsonNode node : response) {
            String round = node.asString();
            if (round != null && !round.isBlank()) {
                rounds.add(round);
            }
        }
        return List.copyOf(rounds);
    }

    List<ProviderFixture> fixtures(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            throw ApiException.providerUnavailable("API-Football returned an unexpected fixtures payload");
        }
        List<ProviderFixture> fixtures = new ArrayList<>();
        for (JsonNode node : response) {
            fixtures.add(toFixture(node));
        }
        return List.copyOf(fixtures);
    }

    ProviderMatchDetails matchDetails(
            JsonNode fixtureRoot,
            List<ProviderMatchDetails.Event> events,
            List<ProviderMatchDetails.Lineup> lineups,
            List<ProviderMatchDetails.TeamStatistics> statistics
    ) {
        assertNoProviderErrors(fixtureRoot);
        JsonNode response = fixtureRoot.path("response");
        if (!response.isArray() || response.size() == 0) {
            return null;
        }
        JsonNode node = response.get(0);
        JsonNode fixture = node.path("fixture");
        JsonNode status = fixture.path("status");
        JsonNode venue = fixture.path("venue");
        JsonNode league = node.path("league");
        JsonNode score = node.path("score");
        return new ProviderMatchDetails(
                toFixture(node),
                league.path("id").asInt(),
                nullIfBlank(league.path("name").asString()),
                integerOrNull(status.path("extra")),
                nullIfBlank(fixture.path("referee").asString()),
                nullIfBlank(venue.path("name").asString()),
                nullIfBlank(venue.path("city").asString()),
                new ProviderMatchDetails.ScoreBreakdown(
                        integerOrNull(score.path("halftime").path("home")),
                        integerOrNull(score.path("halftime").path("away")),
                        integerOrNull(score.path("fulltime").path("home")),
                        integerOrNull(score.path("fulltime").path("away")),
                        integerOrNull(score.path("extratime").path("home")),
                        integerOrNull(score.path("extratime").path("away")),
                        integerOrNull(score.path("penalty").path("home")),
                        integerOrNull(score.path("penalty").path("away"))
                ),
                events,
                lineups,
                statistics
        );
    }

    ProviderStandings standings(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray() || response.size() == 0) {
            return null;
        }
        JsonNode league = response.get(0).path("league");
        List<ProviderStanding> table = new ArrayList<>();
        JsonNode groups = league.path("standings");
        if (groups.isArray() && groups.size() > 0) {
            JsonNode firstTable = groups.get(0);
            if (firstTable.isArray()) {
                for (JsonNode row : firstTable) {
                    table.add(toStanding(row));
                }
            }
        }
        table.sort(Comparator.comparingInt(ProviderStanding::position));
        Instant updatedAt = table.stream()
                .map(ProviderStanding::updatedAt)
                .filter(instant -> instant != null)
                .findFirst()
                .orElse(null);
        return new ProviderStandings(
                league.path("id").asInt(),
                nullIfBlank(league.path("name").asString()),
                league.path("season").asInt(),
                updatedAt,
                List.copyOf(table)
        );
    }

    List<ProviderMatchDetails.Event> events(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            return List.of();
        }
        List<ProviderMatchDetails.Event> events = new ArrayList<>();
        for (JsonNode node : response) {
            JsonNode time = node.path("time");
            JsonNode team = node.path("team");
            JsonNode player = node.path("player");
            JsonNode assist = node.path("assist");
            events.add(new ProviderMatchDetails.Event(
                    integerOrNull(time.path("elapsed")),
                    integerOrNull(time.path("extra")),
                    team.path("id").asLong(),
                    nullIfBlank(team.path("name").asString()),
                    longOrNull(player.path("id")),
                    nullIfBlank(player.path("name").asString()),
                    longOrNull(assist.path("id")),
                    nullIfBlank(assist.path("name").asString()),
                    nullIfBlank(node.path("type").asString()),
                    nullIfBlank(node.path("detail").asString()),
                    nullIfBlank(node.path("comments").asString())
            ));
        }
        return List.copyOf(events);
    }

    List<ProviderMatchDetails.Lineup> lineups(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            return List.of();
        }
        List<ProviderMatchDetails.Lineup> lineups = new ArrayList<>();
        for (JsonNode node : response) {
            JsonNode team = node.path("team");
            lineups.add(new ProviderMatchDetails.Lineup(
                    team.path("id").asLong(),
                    nullIfBlank(team.path("name").asString()),
                    nullIfBlank(team.path("logo").asString()),
                    nullIfBlank(node.path("formation").asString()),
                    nullIfBlank(node.path("coach").path("name").asString()),
                    players(node.path("startXI")),
                    players(node.path("substitutes"))
            ));
        }
        return List.copyOf(lineups);
    }

    List<ProviderMatchDetails.TeamStatistics> statistics(JsonNode root) {
        assertNoProviderErrors(root);
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            return List.of();
        }
        List<ProviderMatchDetails.TeamStatistics> statistics = new ArrayList<>();
        for (JsonNode node : response) {
            JsonNode team = node.path("team");
            List<ProviderMatchDetails.Stat> stats = new ArrayList<>();
            JsonNode items = node.path("statistics");
            if (items.isArray()) {
                for (JsonNode stat : items) {
                    stats.add(new ProviderMatchDetails.Stat(
                            nullIfBlank(stat.path("type").asString()),
                            stringOrNull(stat.path("value"))
                    ));
                }
            }
            statistics.add(new ProviderMatchDetails.TeamStatistics(
                    team.path("id").asLong(),
                    nullIfBlank(team.path("name").asString()),
                    List.copyOf(stats)
            ));
        }
        return List.copyOf(statistics);
    }

    private static List<ProviderMatchDetails.LineupPlayer> players(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<ProviderMatchDetails.LineupPlayer> players = new ArrayList<>();
        for (JsonNode node : array) {
            JsonNode player = node.path("player");
            players.add(new ProviderMatchDetails.LineupPlayer(
                    player.path("id").asLong(),
                    nullIfBlank(player.path("name").asString()),
                    integerOrNull(player.path("number")),
                    nullIfBlank(player.path("pos").asString()),
                    nullIfBlank(player.path("grid").asString())
            ));
        }
        return List.copyOf(players);
    }

    private static ProviderStanding toStanding(JsonNode row) {
        JsonNode team = row.path("team");
        JsonNode all = row.path("all");
        JsonNode goals = all.path("goals");
        return new ProviderStanding(
                row.path("rank").asInt(),
                team.path("id").asLong(),
                nullIfBlank(team.path("name").asString()),
                nullIfBlank(team.path("logo").asString()),
                row.path("points").asInt(),
                all.path("played").asInt(),
                all.path("win").asInt(),
                all.path("draw").asInt(),
                all.path("lose").asInt(),
                goals.path("for").asInt(),
                goals.path("against").asInt(),
                row.path("goalsDiff").asInt(),
                parseInstant(nullIfBlank(row.path("update").asString()))
        );
    }

    private static ProviderFixture toFixture(JsonNode node) {
        JsonNode fixture = node.path("fixture");
        JsonNode status = fixture.path("status");
        JsonNode teams = node.path("teams");
        JsonNode home = teams.path("home");
        JsonNode away = teams.path("away");
        JsonNode goals = node.path("goals");
        return new ProviderFixture(
                fixture.path("id").asLong(),
                nullIfBlank(node.path("league").path("round").asString()),
                parseInstant(fixture.path("date").asString()),
                nullIfBlank(status.path("short").asString()),
                nullIfBlank(status.path("long").asString()),
                integerOrNull(status.path("elapsed")),
                home.path("id").asLong(),
                nullIfBlank(home.path("name").asString()),
                nullIfBlank(home.path("logo").asString()),
                away.path("id").asLong(),
                nullIfBlank(away.path("name").asString()),
                nullIfBlank(away.path("logo").asString()),
                integerOrNull(goals.path("home")),
                integerOrNull(goals.path("away"))
        );
    }

    static void assertNoProviderErrors(JsonNode root) {
        JsonNode errors = root.path("errors");
        if (errors.isMissingNode() || errors.isNull()) {
            return;
        }
        if (errors.isArray() && errors.size() == 0) {
            return;
        }
        if (errors.isObject() && errors.size() == 0) {
            return;
        }
        throw ApiException.providerUnavailable("API-Football returned an error: " + summarizeErrors(errors));
    }

    private static String summarizeErrors(JsonNode errors) {
        if (errors.isTextual()) {
            return errors.asString();
        }
        if (errors.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode node : errors) {
                parts.add(node.asString());
            }
            return String.join("; ", parts);
        }
        if (errors.isObject()) {
            List<String> parts = new ArrayList<>();
            errors.properties().forEach(entry -> parts.add(entry.getKey() + ": " + entry.getValue().asString()));
            return String.join("; ", parts);
        }
        return "unavailable data";
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return Instant.parse(value.replace(" ", "T"));
        }
    }

    private static Long longOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }

    private static String stringOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asString();
        }
        return nullIfBlank(node.asString());
    }

    private static Integer integerOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber() || node.isString()) {
            return node.asInt();
        }
        return null;
    }

    private static String nullIfBlank(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }
}
