package com.placementtracker.dto;

import com.placementtracker.model.DriveApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriveApprovalUpdateRequest {

    // Only APPROVED or REJECTED are meaningful here - moving to/through DRAFT or
    // PENDING_APPROVAL is the Interviewer's own action (create/submit), not Admin's.
    @NotNull
    private DriveApprovalStatus approvalStatus;

    private String rejectionReason;
}
