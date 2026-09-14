package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.InterviewerScoreSubmitRequest;
import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.RoundResult;
import com.placementtracker.repository.PanelAssignmentRepository;
import com.placementtracker.repository.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewerScoreService {

    private final PanelAssignmentRepository panelAssignmentRepository;
    private final RoundResultRepository roundResultRepository;
    private final RoundResultService roundResultService;

    // The server-side enforcement of "an interviewer can only score their own assigned
    // students": a PanelAssignment(interviewer, roundResultId) row must exist, or this 404s
    // the same way the student self-submit path 404s on someone else's round result - never
    // revealing that the round result exists at all. Even a hand-crafted request naming an
    // arbitrary roundResultId is rejected unless a real assignment says otherwise.
    public RoundResultResponse submitScore(User interviewer, Long roundResultId, InterviewerScoreSubmitRequest request) {
        if (!panelAssignmentRepository.existsByInterviewerIdAndRoundResultId(interviewer.getId(), roundResultId)) {
            throw new ResourceNotFoundException("No round result found with id " + roundResultId);
        }
        RoundResult roundResult = roundResultRepository.findById(roundResultId)
                .orElseThrow(() -> new ResourceNotFoundException("No round result found with id " + roundResultId));
        return roundResultService.scoreRoundResult(roundResult, request.getScore(), request.getRemarks());
    }
}
