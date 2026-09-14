package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.RoundFinalizeResultResponse;
import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.dto.RoundResultUpdateRequest;
import com.placementtracker.dto.RoundScoreSubmitRequest;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.ApplicationStatus;
import com.placementtracker.model.Round;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.model.SelectionMode;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.RoundRepository;
import com.placementtracker.repository.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundResultService {

    private final RoundResultRepository roundResultRepository;
    private final ApplicationRepository applicationRepository;
    private final RoundRepository roundRepository;

    public RoundResultResponse update(Long roundResultId, RoundResultUpdateRequest request) {
        RoundResult roundResult = getRoundResultOrThrow(roundResultId);
        assertPriorRoundsPassed(roundResult);
        applyStatusChange(roundResult, request.getStatus(), request.getRemarks());
        return new RoundResultResponse(roundResult);
    }

    // A student submits their own score for a round configured for self-score-submission
    // (Round.minScore != null). Delegates to scoreRoundResult, the same entry point every
    // other score-recording caller (CSV import, interviewer entry) uses.
    public RoundResultResponse submitScore(User user, Long roundResultId, RoundScoreSubmitRequest request) {
        RoundResult roundResult = getRoundResultOrThrow(roundResultId);

        if (!roundResult.getApplication().getStudent().getUser().getId().equals(user.getId())) {
            // Don't reveal that a round result with this id exists for someone else.
            throw new ResourceNotFoundException("No round result found with id " + roundResultId);
        }

        return scoreRoundResult(roundResult, request.getScore(), roundResult.getRemarks());
    }

    // Records a score against a round result and resolves its status. This is the single
    // shared entry point for every score-recording caller - student self-submit, admin CSV
    // bulk import, and interviewer live entry - so there is exactly one place the
    // threshold/ranking rules live. Caller is responsible for any ownership/assignment check
    // specific to who's allowed to call it.
    public RoundResultResponse scoreRoundResult(RoundResult roundResult, BigDecimal score, String remarks) {
        assertPriorRoundsPassed(roundResult);

        if (roundResult.getStatus() != RoundResultStatus.PENDING && roundResult.getStatus() != RoundResultStatus.SCORED) {
            throw new ApplicationNotAllowedException("This round has already been graded");
        }

        roundResult.setScore(score);

        Round round = roundResult.getRound();
        if (round.getEffectiveSelectionMode() == SelectionMode.THRESHOLD) {
            BigDecimal minScore = round.getMinScore();
            if (minScore == null) {
                throw new ApplicationNotAllowedException(
                        "This round is graded by the placement team and does not accept a self-submitted score");
            }
            RoundResultStatus status = score.compareTo(minScore) >= 0
                    ? RoundResultStatus.PASSED
                    : RoundResultStatus.FAILED;
            applyStatusChange(roundResult, status, remarks);
        } else {
            // Ranking-based rounds (TOP_N / THRESHOLD_THEN_TOP_N) can't decide any one
            // student's outcome from a single score - the round must be finalized first
            // (see finalizeRound). The score is recorded and the result marked SCORED,
            // which - unlike PASSED - still keeps every later round locked.
            applyStatusChange(roundResult, RoundResultStatus.SCORED, remarks);
        }

        return new RoundResultResponse(roundResult);
    }

    // One-shot: ranks every scored result in a TOP_N / THRESHOLD_THEN_TOP_N round and
    // resolves PASSED/FAILED for all of them at once, since ranking-based outcomes can't be
    // decided per-submission the way threshold grading can. Results already manually
    // overridden to PASSED/FAILED are left untouched; PENDING (never scored - a no-show) is
    // treated as an automatic FAILED.
    public RoundFinalizeResultResponse finalizeRound(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("No round found with id " + roundId));

        if (round.getEffectiveSelectionMode() == SelectionMode.THRESHOLD) {
            throw new ApplicationNotAllowedException("This round grades instantly and does not need finalizing");
        }
        if (round.getFinalizedAt() != null) {
            throw new ApplicationNotAllowedException("This round has already been finalized");
        }

        List<RoundResult> results = roundResultRepository.findByRoundId(roundId);
        List<RoundResult> rankingPool = new ArrayList<>();

        for (RoundResult rr : results) {
            if (rr.getStatus() == RoundResultStatus.PASSED || rr.getStatus() == RoundResultStatus.FAILED) {
                continue;
            }
            if (rr.getStatus() == RoundResultStatus.PENDING) {
                applyStatusChange(rr, RoundResultStatus.FAILED, rr.getRemarks());
                continue;
            }
            // SCORED
            if (round.getEffectiveSelectionMode() == SelectionMode.THRESHOLD_THEN_TOP_N
                    && rr.getScore().compareTo(round.getMinScore()) < 0) {
                applyStatusChange(rr, RoundResultStatus.FAILED, rr.getRemarks());
                continue;
            }
            rankingPool.add(rr);
        }

        // Rank by score descending; the top topN pass. Ties at the cutoff all pass rather
        // than an arbitrary tiebreak, so this may pass slightly more than topN students.
        rankingPool.sort(Comparator.comparing(RoundResult::getScore).reversed());
        Integer topN = round.getTopN();
        BigDecimal cutoff = rankingPool.size() <= topN ? null : rankingPool.get(topN - 1).getScore();
        for (RoundResult rr : rankingPool) {
            RoundResultStatus status = cutoff == null || rr.getScore().compareTo(cutoff) >= 0
                    ? RoundResultStatus.PASSED
                    : RoundResultStatus.FAILED;
            applyStatusChange(rr, status, rr.getRemarks());
        }

        round.setFinalizedAt(LocalDateTime.now());
        roundRepository.save(round);

        long passedCount = results.stream().filter(rr -> rr.getStatus() == RoundResultStatus.PASSED).count();
        long failedCount = results.stream().filter(rr -> rr.getStatus() == RoundResultStatus.FAILED).count();
        return new RoundFinalizeResultResponse(round.getId(), passedCount, failedCount, results.size(), round.getFinalizedAt());
    }

    private void applyStatusChange(RoundResult roundResult, RoundResultStatus status, String remarks) {
        roundResult.setStatus(status);
        roundResult.setRemarks(remarks);
        roundResultRepository.save(roundResult);

        Application application = roundResult.getApplication();
        if (status == RoundResultStatus.FAILED) {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
        } else if (status == RoundResultStatus.PASSED
                && application.getStatus() == ApplicationStatus.APPLIED) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
            applicationRepository.save(application);
        }
    }

    private RoundResult getRoundResultOrThrow(Long roundResultId) {
        return roundResultRepository.findById(roundResultId)
                .orElseThrow(() -> new ResourceNotFoundException("No round result found with id " + roundResultId));
    }

    // Enforces sequential progression: a round can only be graded once every earlier round
    // (by sequence, within the same application) has been PASSED. Ties in sequence are
    // treated as parallel/same-stage rounds, not prerequisites of each other.
    private void assertPriorRoundsPassed(RoundResult roundResult) {
        if (!priorRoundsPassed(roundResult)) {
            throw new ApplicationNotAllowedException(
                    "Cannot record a result for this round until the student has passed all earlier rounds");
        }
    }

    // Reused by CSV export/preview (exclude rows that aren't legally gradable yet) and the
    // funnel dashboard ("reached this round" = every earlier round passed).
    public boolean priorRoundsPassed(RoundResult roundResult) {
        int sequence = roundResult.getRound().getSequence();
        return roundResultRepository.findByApplicationIdOrderByRound_SequenceAsc(roundResult.getApplication().getId())
                .stream()
                .filter(rr -> rr.getRound().getSequence() < sequence)
                .noneMatch(rr -> rr.getStatus() != RoundResultStatus.PASSED);
    }
}
