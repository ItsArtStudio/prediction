package org.java5thsem.predictions.prediction;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.match.MatchRepository;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final Clock clock;

    public PredictionService(
            PredictionRepository predictionRepository,
            UserRepository userRepository,
            MatchRepository matchRepository,
            Clock clock
    ) {
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    @Transactional
    public PredictionResponse submit(Long userId, Long matchId, PredictionRequest request) {
        User user = requireUser(userId);
        Match match = requireMatch(matchId);
        ensureOpenForPredictions(match);

        if (predictionRepository.existsByUser_IdAndMatch_Id(userId, matchId)) {
            throw ApiException.duplicatePrediction(userId, matchId);
        }

        Prediction prediction = new Prediction(user, match, request.homeScore(), request.awayScore());
        try {
            return PredictionResponse.from(predictionRepository.saveAndFlush(prediction));
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.duplicatePrediction(userId, matchId);
        }
    }

    @Transactional
    public PredictionResponse update(Long userId, Long matchId, PredictionRequest request) {
        requireUser(userId);
        Match match = requireMatch(matchId);
        ensureOpenForPredictions(match);

        Prediction prediction = predictionRepository.findByUser_IdAndMatch_Id(userId, matchId)
                .orElseThrow(() -> ApiException.predictionNotFound(userId, matchId));
        prediction.updateScore(request.homeScore(), request.awayScore());
        return PredictionResponse.from(predictionRepository.saveAndFlush(prediction));
    }

    @Transactional(readOnly = true)
    public PredictionResponse getCurrent(Long userId, Long matchId) {
        requireUser(userId);
        requireMatch(matchId);
        return predictionRepository.findByUser_IdAndMatch_Id(userId, matchId)
                .map(PredictionResponse::from)
                .orElseThrow(() -> ApiException.predictionNotFound(userId, matchId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.userNotFound(userId));
    }

    private Match requireMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> ApiException.matchNotFound(matchId));
    }

    private void ensureOpenForPredictions(Match match) {
        Instant now = Instant.now(clock);
        if (match.hasStarted(now)) {
            throw ApiException.matchAlreadyStarted(match.getId());
        }
        if (!match.isAvailableForPredictions()) {
            throw ApiException.matchNotAvailable(match.getId());
        }
        if (match.isAfterDeadline(now)) {
            throw ApiException.deadlinePassed(match.getId());
        }
    }
}
