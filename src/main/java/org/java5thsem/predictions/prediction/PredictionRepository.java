package org.java5thsem.predictions.prediction;

import org.java5thsem.predictions.match.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUser_IdAndMatch_Id(Long userId, Long matchId);

    boolean existsByUser_IdAndMatch_Id(Long userId, Long matchId);

    @Query("""
            select p from Prediction p
            join fetch p.user
            join fetch p.match m
            where m.status = :status
              and m.resultHomeScore is not null
              and m.resultAwayScore is not null
            """)
    List<Prediction> findAllForScoring(@Param("status") MatchStatus status);
}
