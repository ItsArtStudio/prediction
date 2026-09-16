package org.java5thsem.predictions.prediction;

import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.match.MatchRepository;
import org.java5thsem.predictions.match.MatchStatus;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PredictionApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    private User user;
    private Match openMatch;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        matchRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("alice-" + System.nanoTime()));
        openMatch = saveMatch(
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS),
                MatchStatus.SCHEDULED,
                true
        );
    }

    @Test
    void submitStoresPredictedScoreForUserAndMatch() throws Exception {
        mockMvc.perform(post(predictionUrl(user.getId(), openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 2, "awayScore": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.matchId").value(openMatch.getId()))
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getReturnsCurrentPrediction() throws Exception {
        submitPrediction(user.getId(), openMatch.getId(), 2, 1);

        mockMvc.perform(get(predictionUrl(user.getId(), openMatch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1));
    }

    @Test
    void updateChangesScoreBeforeDeadline() throws Exception {
        submitPrediction(user.getId(), openMatch.getId(), 2, 1);
        Thread.sleep(20);

        mockMvc.perform(put(predictionUrl(user.getId(), openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 3, "awayScore": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeScore").value(3))
                .andExpect(jsonPath("$.awayScore").value(0))
                .andExpect(jsonPath("$.updatedAt").exists());

        MvcResult result = mockMvc.perform(get(predictionUrl(user.getId(), openMatch.getId())))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).contains("\"homeScore\":3");
    }

    @Test
    void duplicatePredictionIsRejected() throws Exception {
        submitPrediction(user.getId(), openMatch.getId(), 2, 1);

        mockMvc.perform(post(predictionUrl(user.getId(), openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PREDICTION"));
    }

    @Test
    void unknownMatchIsRejected() throws Exception {
        mockMvc.perform(post(predictionUrl(user.getId(), 999_999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        mockMvc.perform(post(predictionUrl(999_999L, openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void missingPredictionReturnsNotFound() throws Exception {
        mockMvc.perform(get(predictionUrl(user.getId(), openMatch.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PREDICTION_NOT_FOUND"));
    }

    @Test
    void cannotCreateAfterMatchHasStarted() throws Exception {
        Match started = saveMatch(
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().minus(20, ChronoUnit.MINUTES),
                MatchStatus.LIVE,
                true
        );

        mockMvc.perform(post(predictionUrl(user.getId(), started.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MATCH_ALREADY_STARTED"));
    }

    @Test
    void cannotUpdateAfterMatchHasStarted() throws Exception {
        submitPrediction(user.getId(), openMatch.getId(), 2, 1);
        Match started = saveMatch(
                Instant.now().minus(5, ChronoUnit.MINUTES),
                Instant.now().plus(1, ChronoUnit.HOURS),
                MatchStatus.SCHEDULED,
                true
        );
        predictionRepository.save(new Prediction(user, started, 1, 0));

        mockMvc.perform(put(predictionUrl(user.getId(), started.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 4, "awayScore": 0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MATCH_ALREADY_STARTED"));
    }

    @Test
    void cannotCreateAfterDeadline() throws Exception {
        Match pastDeadline = saveMatch(
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().minus(5, ChronoUnit.MINUTES),
                MatchStatus.SCHEDULED,
                true
        );

        mockMvc.perform(post(predictionUrl(user.getId(), pastDeadline.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PREDICTION_DEADLINE_PASSED"));
    }

    @Test
    void cannotUpdateAfterDeadline() throws Exception {
        Match closingSoon = saveMatch(
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS),
                MatchStatus.SCHEDULED,
                true
        );
        submitPrediction(user.getId(), closingSoon.getId(), 1, 0);

        Match closed = saveMatch(
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.MINUTES),
                MatchStatus.SCHEDULED,
                true
        );
        predictionRepository.save(new Prediction(user, closed, 1, 0));

        mockMvc.perform(put(predictionUrl(user.getId(), closed.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 5, "awayScore": 5}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PREDICTION_DEADLINE_PASSED"));
    }

    @Test
    void unavailableMatchIsRejected() throws Exception {
        Match unavailable = saveMatch(
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS),
                MatchStatus.SCHEDULED,
                false
        );

        mockMvc.perform(post(predictionUrl(user.getId(), unavailable.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1, "awayScore": 0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_AVAILABLE"));
    }

    @Test
    void negativeScoresAreRejected() throws Exception {
        mockMvc.perform(post(predictionUrl(user.getId(), openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": -1, "awayScore": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingScoresAreRejected() throws Exception {
        mockMvc.perform(post(predictionUrl(user.getId(), openMatch.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()", greaterThanOrEqualTo(1)));
    }

    private Match saveMatch(Instant kickoffAt, Instant deadline, MatchStatus status, boolean enabled) {
        return matchRepository.save(new Match("Arsenal", "Chelsea", kickoffAt, deadline, status, enabled));
    }

    private void submitPrediction(Long userId, Long matchId, int homeScore, int awayScore) throws Exception {
        mockMvc.perform(post(predictionUrl(userId, matchId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeScore": %d, "awayScore": %d}
                                """.formatted(homeScore, awayScore)))
                .andExpect(status().isCreated());
    }

    private static String predictionUrl(Long userId, Long matchId) {
        return "/api/users/%d/matches/%d/prediction".formatted(userId, matchId);
    }
}
