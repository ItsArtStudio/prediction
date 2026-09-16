package org.java5thsem.predictions.leaderboard;

import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.match.MatchRepository;
import org.java5thsem.predictions.match.MatchStatus;
import org.java5thsem.predictions.prediction.Prediction;
import org.java5thsem.predictions.prediction.PredictionRepository;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaderboardApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        matchRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("alice"));
        bob = userRepository.save(new User("bob"));
        charlie = userRepository.save(new User("charlie"));
    }

    @Test
    void emptyLeaderboardWhenNoFinishedPredictions() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoring.exactScorePoints").value(3))
                .andExpect(jsonPath("$.scoring.correctOutcomePoints").value(1))
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    void ranksUsersByPointsFromFinishedMatches() throws Exception {
        Match finished = finishedMatch(2, 1);
        predictionRepository.save(new Prediction(alice, finished, 2, 1));
        predictionRepository.save(new Prediction(bob, finished, 1, 0));
        predictionRepository.save(new Prediction(charlie, finished, 0, 3));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(3))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.entries[0].points").value(3))
                .andExpect(jsonPath("$.entries[0].exactScores").value(1))
                .andExpect(jsonPath("$.entries[1].rank").value(2))
                .andExpect(jsonPath("$.entries[1].username").value("bob"))
                .andExpect(jsonPath("$.entries[1].points").value(1))
                .andExpect(jsonPath("$.entries[1].correctOutcomes").value(1))
                .andExpect(jsonPath("$.entries[2].rank").value(3))
                .andExpect(jsonPath("$.entries[2].username").value("charlie"))
                .andExpect(jsonPath("$.entries[2].points").value(0));
    }

    @Test
    void ignoresPredictionsForUnfinishedMatches() throws Exception {
        Match finished = finishedMatch(1, 0);
        Match live = matchRepository.save(new Match(
                "Liverpool",
                "Everton",
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().minus(20, ChronoUnit.MINUTES),
                MatchStatus.LIVE,
                true
        ));
        predictionRepository.save(new Prediction(alice, finished, 1, 0));
        predictionRepository.save(new Prediction(bob, live, 3, 0));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.entries[0].predictionsScored").value(1));
    }

    @Test
    void equalPointsShareRank() throws Exception {
        Match first = finishedMatch(2, 1);
        Match second = finishedMatch(0, 0);
        predictionRepository.save(new Prediction(alice, first, 2, 1));
        predictionRepository.save(new Prediction(bob, second, 0, 0));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].points").value(3))
                .andExpect(jsonPath("$.entries[1].rank").value(1))
                .andExpect(jsonPath("$.entries[1].points").value(3));
    }

    @Test
    void sumsPointsAcrossMultipleFinishedMatches() throws Exception {
        Match first = finishedMatch(2, 1);
        Match second = finishedMatch(0, 0);
        predictionRepository.save(new Prediction(alice, first, 2, 1));
        predictionRepository.save(new Prediction(alice, second, 1, 1));
        predictionRepository.save(new Prediction(bob, first, 0, 1));
        predictionRepository.save(new Prediction(bob, second, 0, 0));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.entries[0].points").value(4))
                .andExpect(jsonPath("$.entries[0].exactScores").value(1))
                .andExpect(jsonPath("$.entries[0].correctOutcomes").value(1))
                .andExpect(jsonPath("$.entries[1].username").value("bob"))
                .andExpect(jsonPath("$.entries[1].points").value(3))
                .andExpect(jsonPath("$.entries[1].rank").value(2));
    }

    private Match finishedMatch(int homeScore, int awayScore) {
        Match match = new Match(
                "Arsenal",
                "Chelsea",
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().minus(2, ChronoUnit.DAYS),
                MatchStatus.FINISHED,
                false
        );
        match.complete(homeScore, awayScore);
        return matchRepository.save(match);
    }
}
