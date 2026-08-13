package com.placementtracker.dto;

import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    }
}
