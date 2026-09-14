package com.placementtracker.service;

import com.placementtracker.dto.DriveFunnelResponse;
import com.placementtracker.dto.FunnelStageResponse;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.Drive;
import com.placementtracker.model.Round;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.DriveRepository;
import com.placementtracker.repository.OfferRepository;
import com.placementtracker.repository.RoundResultRepository;
import com.placementtracker.util.RoundResultLockCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Read-only, per-Drive (not company-wide) - rounds/eligibility are Drive-scoped, and
// different drives under one company can have entirely different round structures, so a
// cross-drive rollup would be lossy.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveFunnelService {

    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final RoundResultRepository roundResultRepository;
    private final OfferRepository offerRepository;

    public DriveFunnelResponse getFunnel(Long driveId) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + driveId));
        List<Application> applications = applicationRepository.findByDriveId(driveId);

        // reached, passed, failed, awaiting - per round, in that fixed order.
        Map<Long, int[]> tallyByRoundId = new LinkedHashMap<>();
        for (Round round : drive.getRounds()) {
            tallyByRoundId.put(round.getId(), new int[4]);
        }

        for (Application application : applications) {
            List<RoundResult> results = roundResultRepository.findByApplicationIdOrderByRound_SequenceAsc(application.getId());
            Map<Long, String> lockReasonByRoundResultId = RoundResultLockCalculator.lockReasons(results);
            for (RoundResult rr : results) {
                int[] tally = tallyByRoundId.get(rr.getRound().getId());
                if (tally == null || lockReasonByRoundResultId.get(rr.getId()) != null) {
                    continue; // not reached yet
                }
                tally[0]++;
                if (rr.getStatus() == RoundResultStatus.PASSED) {
                    tally[1]++;
                } else if (rr.getStatus() == RoundResultStatus.FAILED) {
                    tally[2]++;
                } else {
                    tally[3]++; // PENDING or SCORED - reached but not yet decided
                }
            }
        }

        List<FunnelStageResponse> stages = drive.getRounds().stream()
                .sorted(Comparator.comparing(Round::getSequence))
                .map(round -> {
                    int[] tally = tallyByRoundId.get(round.getId());
                    return new FunnelStageResponse(round.getId(), round.getSequence(), round.getName(), tally[0], tally[1], tally[2], tally[3]);
                })
                .toList();

        long offeredCount = offerRepository.countByApplication_Drive_Id(driveId);
        return new DriveFunnelResponse(driveId, applications.size(), stages, offeredCount);
    }
}
