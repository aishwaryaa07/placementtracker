package com.placementtracker.dto;

import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import lombok.Getter;

// Purpose-built for the admin panel-assignment picker: which candidates exist in this
// round's pool, with enough student identity to pick from (neither ApplicationResponse nor
// RoundResultResponse expose student name/email today).
@Getter
public class RoundCandidateResponse {

    private final Long roundResultId;
    private final String studentName;
    private final String studentEmail;
    private final RoundResultStatus status;
    private final boolean locked;
    // True once this exact round result has a final PASSED/FAILED outcome - distinct from
    // `locked` (which only means an *earlier* round hasn't been passed yet). A decided
    // result has nothing left for an interviewer to grade, so the picker treats it the same
    // as locked: visible for reference, not selectable.
    private final boolean alreadyDecided;

    public RoundCandidateResponse(RoundResult roundResult, boolean locked) {
        this.roundResultId = roundResult.getId();
        this.studentName = roundResult.getApplication().getStudent().getUser().getName();
        this.studentEmail = roundResult.getApplication().getStudent().getUser().getEmail();
        this.status = roundResult.getStatus();
        this.locked = locked;
        this.alreadyDecided = roundResult.getStatus() == RoundResultStatus.PASSED || roundResult.getStatus() == RoundResultStatus.FAILED;
    }
}
