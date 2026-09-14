package com.placementtracker.repository;

import com.placementtracker.model.RoundResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {
    // Ordered by round sequence - without this, Postgres can return rows in a different
    // order after an UPDATE relocates a row's physical position, which would silently
    // scramble the round order shown in both the Admin and Student UI (they render
    // whatever order the API returns, since it's expected to already be in sequence).
    List<RoundResult> findByApplicationIdOrderByRound_SequenceAsc(Long applicationId);

    List<RoundResult> findByRoundId(Long roundId);

    boolean existsByRoundId(Long roundId);

    boolean existsByApplicationIdAndRoundId(Long applicationId, Long roundId);
}
