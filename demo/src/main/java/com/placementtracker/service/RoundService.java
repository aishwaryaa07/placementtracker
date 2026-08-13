package com.placementtracker.service;

import com.placementtracker.dto.RoundRequest;
import com.placementtracker.dto.RoundResponse;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Drive;
import com.placementtracker.model.Round;
import com.placementtracker.repository.DriveRepository;
import com.placementtracker.repository.RoundRepository;
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

    public RoundResponse create(Long driveId, RoundRequest request) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + driveId));

        Round round = new Round();
        round.setDrive(drive);
        applyRequest(round, request);
        return new RoundResponse(roundRepository.save(round));
    }

    @Transactional(readOnly = true)
    public List<RoundResponse> findByDrive(Long driveId) {
        return roundRepository.findByDriveIdOrderBySequenceAsc(driveId).stream().map(RoundResponse::new).toList();
    }

    public RoundResponse update(Long roundId, RoundRequest request) {
        Round round = getRoundOrThrow(roundId);
        applyRequest(round, request);
        return new RoundResponse(roundRepository.save(round));
    }

    public void delete(Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("No round found with id " + roundId);
        }
        roundRepository.deleteById(roundId);
    }

    private Round getRoundOrThrow(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("No round found with id " + roundId));
    }

    private void applyRequest(Round round, RoundRequest request) {
        round.setSequence(request.getSequence());
        round.setName(request.getName());
        round.setRoundDate(request.getRoundDate());
    }
}
