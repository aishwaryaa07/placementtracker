package com.placementtracker.dto;

import com.placementtracker.model.PanelAssignment;
import com.placementtracker.model.RoundResultStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PanelAssignmentResponse {

    private final Long id;
    private final Long interviewerId;
    private final String interviewerName;
    private final Long roundResultId;
    private final String studentName;
    private final String studentEmail;
    private final String roundName;
    private final Integer roundSequence;
    private final RoundResultStatus status;
    private final LocalDateTime assignedAt;

    public PanelAssignmentResponse(PanelAssignment assignment) {
        this.id = assignment.getId();
        this.interviewerId = assignment.getInterviewer().getId();
        this.interviewerName = assignment.getInterviewer().getName();
        this.roundResultId = assignment.getRoundResult().getId();
        this.studentName = assignment.getRoundResult().getApplication().getStudent().getUser().getName();
        this.studentEmail = assignment.getRoundResult().getApplication().getStudent().getUser().getEmail();
        this.roundName = assignment.getRoundResult().getRound().getName();
        this.roundSequence = assignment.getRoundResult().getRound().getSequence();
        this.status = assignment.getRoundResult().getStatus();
        this.assignedAt = assignment.getAssignedAt();
    }
}
