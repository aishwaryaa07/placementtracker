package com.placementtracker.service;

import com.placementtracker.dto.RoundRequest;
import com.placementtracker.dto.RoundResponse;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.Drive;
import com.placementtracker.model.Round;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.SelectionMode;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.DriveRepository;
import com.placementtracker.repository.RoundRepository;
import com.placementtracker.repository.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundService {

    private final RoundRepository roundRepository;
    private final DriveRepository driveRepository;
    private final RoundResultRepository roundResultRepository;
    private final ApplicationRepository applicationRepository;

    public RoundResponse create(Long driveId, RoundRequest request) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + driveId));

        assertSequenceNotTaken(driveId, request.getSequence(), null);
        assertSelectionModeValid(request);

        Round round = new Round();
        round.setDrive(drive);
        applyRequest(round, request);
        Round saved = roundRepository.save(round);

        backfillRoundResultsForExistingApplications(driveId, saved);

        return new RoundResponse(saved);
    }

    // A round added after students have already applied would otherwise never appear for
    // them - RoundResult rows are normally only created once, at apply() time, from
    // whichever rounds existed then (see ApplicationService.apply). Without this, an
    // already-SELECTED application with no rounds yet would have no way to ever get one.
    private void backfillRoundResultsForExistingApplications(Long driveId, Round round) {
        List<Application> existingApplications = applicationRepository.findByDriveId(driveId);
        List<RoundResult> newResults = existingApplications.stream()
                .filter(app -> !roundResultRepository.existsByApplicationIdAndRoundId(app.getId(), round.getId()))
                .map(app -> {
                    RoundResult result = new RoundResult();
                    result.setApplication(app);
                    result.setRound(round);
                    return result;
                })
                .toList();
        roundResultRepository.saveAll(newResults);
    }

    @Transactional(readOnly = true)
    public List<RoundResponse> findByDrive(Long driveId) {
        if (!driveRepository.existsById(driveId)) {
            throw new ResourceNotFoundException("No drive found with id " + driveId);
        }
        return roundRepository.findByDriveIdOrderBySequenceAsc(driveId).stream().map(RoundResponse::new).toList();
    }

    public RoundResponse update(Long roundId, RoundRequest request) {
        Round round = getRoundOrThrow(roundId);
        assertSequenceNotTaken(round.getDrive().getId(), request.getSequence(), roundId);
        assertSelectionModeValid(request);
        applyRequest(round, request);
        return new RoundResponse(roundRepository.save(round));
    }

    public void delete(Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("No round found with id " + roundId);
        }
        // Every application to this round's drive gets a RoundResult row for every round
        // up front (see ApplicationService.apply), so a round with any applicants - even
        // ungraded ones - has round_results rows FK'd to it. Deleting it would previously
        // hit that FK constraint and surface as a raw 500; reject it cleanly instead.
        if (roundResultRepository.existsByRoundId(roundId)) {
            throw new ApplicationNotAllowedException(
                    "Cannot delete a round that already has applicant results");
        }
        roundRepository.deleteById(roundId);
    }

    private Round getRoundOrThrow(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("No round found with id " + roundId));
    }

    // Backend-level backstop for the same rule the admin UI already checks client-side:
    // two rounds in the same drive can't share a sequence number. excludingRoundId lets an
    // update keep its own current sequence without tripping over itself.
    private void assertSequenceNotTaken(Long driveId, Integer sequence, Long excludingRoundId) {
        boolean taken = roundRepository.findByDriveIdOrderBySequenceAsc(driveId).stream()
                .anyMatch(r -> r.getSequence().equals(sequence) && !r.getId().equals(excludingRoundId));
        if (taken) {
            throw new ApplicationNotAllowedException(
                    "Sequence " + sequence + " is already used by another round in this drive");
        }
    }

    private void applyRequest(Round round, RoundRequest request) {
        round.setSequence(request.getSequence());
        round.setName(request.getName());
        round.setRoundDate(request.getRoundDate());
        round.setDescription(request.getDescription());
        round.setMinScore(request.getMinScore());
        round.setSelectionMode(request.getSelectionMode());
        round.setTopN(request.getTopN());
    }

    // TOP_N/THRESHOLD_THEN_TOP_N can't rank anyone without knowing how many should pass;
    // THRESHOLD_THEN_TOP_N additionally needs minScore as its pre-ranking floor.
    private void assertSelectionModeValid(RoundRequest request) {
        SelectionMode mode = request.getSelectionMode();
        if (mode == null || mode == SelectionMode.THRESHOLD) {
            return;
        }
        if (request.getTopN() == null) {
            throw new ApplicationNotAllowedException("Top N is required for this selection mode");
        }
        if (mode == SelectionMode.THRESHOLD_THEN_TOP_N && request.getMinScore() == null) {
            throw new ApplicationNotAllowedException(
                    "A minimum score is required as the pre-ranking floor for this selection mode");
        }
    }
}
