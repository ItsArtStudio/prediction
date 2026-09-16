package org.java5thsem.predictions.config;

import org.java5thsem.predictions.match.Match;
import org.java5thsem.predictions.match.MatchRepository;
import org.java5thsem.predictions.match.MatchStatus;
import org.java5thsem.predictions.prediction.Prediction;
import org.java5thsem.predictions.prediction.PredictionRepository;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Profile("!test")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final Clock clock;

    public DemoDataLoader(
            UserRepository userRepository,
            MatchRepository matchRepository,
            PredictionRepository predictionRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.predictionRepository = predictionRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now(clock);
        User alice = userRepository.save(new User("alice"));
        User bob = userRepository.save(new User("bob"));
        User charlie = userRepository.save(new User("charlie"));

        Match openMatch = matchRepository.save(new Match(
                "Arsenal",
                "Chelsea",
                now.plus(2, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS),
                MatchStatus.SCHEDULED,
                true
        ));
        Match startedMatch = matchRepository.save(new Match(
                "Liverpool",
                "Everton",
                now.minus(1, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.HOURS),
                MatchStatus.LIVE,
                true
        ));
        Match closedMatch = matchRepository.save(new Match(
                "Tottenham",
                "West Ham",
                now.plus(3, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                MatchStatus.SCHEDULED,
                false
        ));

        Match cityUnited = finishedMatch("Man City", "Man United", now.minus(3, ChronoUnit.DAYS), 2, 1);
        Match brightonPalace = finishedMatch("Brighton", "Crystal Palace", now.minus(2, ChronoUnit.DAYS), 0, 0);

        predictionRepository.save(new Prediction(alice, cityUnited, 2, 1));
        predictionRepository.save(new Prediction(bob, cityUnited, 1, 0));
        predictionRepository.save(new Prediction(charlie, cityUnited, 0, 2));

        predictionRepository.save(new Prediction(alice, brightonPalace, 1, 0));
        predictionRepository.save(new Prediction(bob, brightonPalace, 0, 0));

        log.info("Demo users: alice id={}, bob id={}, charlie id={}", alice.getId(), bob.getId(), charlie.getId());
        log.info("Open match (predictions allowed): id={}", openMatch.getId());
        log.info("Started match (predictions locked): id={}", startedMatch.getId());
        log.info("Closed match (not available): id={}", closedMatch.getId());
        log.info("Finished matches for leaderboard: city/united id={}, brighton/palace id={}",
                cityUnited.getId(), brightonPalace.getId());
    }

    private Match finishedMatch(String homeTeam, String awayTeam, Instant kickoffAt, int homeScore, int awayScore) {
        Match match = new Match(
                homeTeam,
                awayTeam,
                kickoffAt,
                kickoffAt.minus(1, ChronoUnit.HOURS),
                MatchStatus.FINISHED,
                false
        );
        match.complete(homeScore, awayScore);
        return matchRepository.save(match);
    }
}
