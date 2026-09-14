package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.PanelAssignmentRequest;
import com.placementtracker.dto.PanelAssignmentResponse;
import com.placementtracker.dto.RoundCandidateResponse;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.PanelAssignment;
import com.placementtracker.model.Role;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.repository.PanelAssignmentRepository;
import com.placementtracker.repository.RoundResultRepository;
import com.placementtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PanelAssignmentService {

    private final PanelAssignmentRepository panelAssignmentRepository;
    private final RoundResultRepository roundResultRepository;
    private final UserRepository userRepository;
    private final RoundResultService roundResultService;

    // Assigns a batch of round results (all within roundId) to one interviewer in a single
    // call - the realistic admin workflow ("assign these N students to Interviewer X for
    // this round"). Already-assigned (interviewer, roundResult) pairs are silently skipped
    // rather than rejected, so re-running the same assignment action is safe.
    public List<PanelAssignmentResponse> assign(Long roundId, PanelAssignmentRequest request) {
        User interviewer = userRepository.findById(request.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with id " + request.getInterviewerId()));
        if (interviewer.getRole() != Role.INTERVIEWER) {
            throw new ApplicationNotAllowedException("This user is not an interviewer");
        }

        for (Long roundResultId : request.getRoundResultIds()) {
            RoundResult roundResult = roundResultRepository.findById(roundResultId)
                    .orElseThrow(() -> new ResourceNotFoundException("No round result found with id " + roundResultId));
            if (!roundResult.getRound().getId().equals(roundId)) {
                throw new ApplicationNotAllowedException(
                        "Round result " + roundResultId + " does not belong to round " + roundId);
            }
            if (!roundResultService.priorRoundsPassed(roundResult)) {
                throw new ApplicationNotAllowedException(
                        "Cannot assign round result " + roundResultId + " - the student hasn't reached this round yet");
            }
            if (roundResult.getStatus() == RoundResultStatus.PASSED || roundResult.getStatus() == RoundResultStatus.FAILED) {
                throw new ApplicationNotAllowedException(
                        "Cannot assign round result " + roundResultId + " - it has already been graded");
            }
            if (panelAssignmentRepository.existsByInterviewerIdAndRoundResultId(interviewer.getId(), roundResultId)) {
                continue;
            }
            PanelAssignment assignment = new PanelAssignment();
            assignment.setInterviewer(interviewer);
            assignment.setRoundResult(roundResult);
            panelAssignmentRepository.save(assignment);
        }

        return findForRound(roundId);
    }

    public void unassign(Long assignmentId) {
        if (!panelAssignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("No panel assignment found with id " + assignmentId);
        }
        panelAssignmentRepository.deleteById(assignmentId);
    }

    // The candidate pool for a round, for the admin panel-assignment picker - the only place
    // student identity is exposed per round result (neither ApplicationResponse nor
    // RoundResultResponse carry it), so the admin can actually see who they're assigning.
    @Transactional(readOnly = true)
    public List<RoundCandidateResponse> findCandidatePool(Long roundId) {
        return roundResultRepository.findByRoundId(roundId).stream()
                .map(rr -> new RoundCandidateResponse(rr, !roundResultService.priorRoundsPassed(rr)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PanelAssignmentResponse> findForRound(Long roundId) {
        return panelAssignmentRepository.findByRoundResult_Round_Id(roundId).stream()
                .map(PanelAssignmentResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PanelAssignmentResponse> findMyAssignments(User interviewer, Long roundId) {
        List<PanelAssignment> assignments = roundId != null
                ? panelAssignmentRepository.findByInterviewerIdAndRoundResult_Round_Id(interviewer.getId(), roundId)
                : panelAssignmentRepository.findByInterviewerId(interviewer.getId());
        return assignments.stream().map(PanelAssignmentResponse::new).toList();
    }
}
