package com.placementtracker.dto;

import com.placementtracker.User;
import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveApprovalStatus;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.model.Qualification;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
public class DriveResponse {

    private final Long id;
    private final CompanyResponse company;
    private final String role;
    private final String description;
    private final BigDecimal ctc;
    private final BigDecimal minCgpa;
    private final Set<String> eligibleBranches;
    private final LocalDate applicationDeadline;
    private final LocalDate driveDate;
    private final DriveStatus status;
    private final List<RoundResponse> rounds;
    private final Long createdById;
    private final String createdByName;
    private final String createdByEmail;
    private final DriveApprovalStatus approvalStatus;
    private final Long approvedById;
    private final String approvedByName;
    private final LocalDateTime approvedAt;
    private final String rejectionReason;
    private final Qualification minQualification;
    private final boolean freshersOnly;

    public DriveResponse(Drive drive) {
        this.id = drive.getId();
        this.company = new CompanyResponse(drive.getCompany());
        this.role = drive.getRole();
        this.description = drive.getDescription();
        this.ctc = drive.getCtc();
        this.minCgpa = drive.getMinCgpa();
        this.eligibleBranches = drive.getEligibleBranches();
        this.applicationDeadline = drive.getApplicationDeadline();
        this.driveDate = drive.getDriveDate();
        this.status = drive.getStatus();
        this.rounds = drive.getRounds().stream().map(RoundResponse::new).toList();

        User createdBy = drive.getCreatedBy();
        this.createdById = createdBy != null ? createdBy.getId() : null;
        this.createdByName = createdBy != null ? createdBy.getName() : null;
        this.createdByEmail = createdBy != null ? createdBy.getEmail() : null;

        User approvedBy = drive.getApprovedBy();
        this.approvedById = approvedBy != null ? approvedBy.getId() : null;
        this.approvedByName = approvedBy != null ? approvedBy.getName() : null;

        this.approvalStatus = drive.getApprovalStatus();
        this.approvedAt = drive.getApprovedAt();
        this.rejectionReason = drive.getRejectionReason();
        this.minQualification = drive.getMinQualification();
        this.freshersOnly = drive.isFreshersOnly();
    }
}
