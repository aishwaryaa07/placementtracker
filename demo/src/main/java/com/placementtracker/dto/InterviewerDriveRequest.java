package com.placementtracker.dto;

import com.placementtracker.model.Qualification;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

// Same shape as DriveRequest minus companyId - an Interviewer drafts under their own linked
// company only, resolved server-side from the authenticated principal, never from client input.
@Getter
@Setter
public class InterviewerDriveRequest {

    @NotBlank
    private String role;

    private String description;

    @DecimalMin(value = "0", message = "CTC cannot be negative")
    private BigDecimal ctc;

    @DecimalMin(value = "1.0", message = "Minimum CGPA must be at least 1.0")
    @DecimalMax(value = "10.0", message = "Minimum CGPA must be at most 10.0")
    @Digits(integer = 2, fraction = 1, message = "Minimum CGPA can have at most one decimal place, e.g. 7.5")
    private BigDecimal minCgpa;

    private Set<String> eligibleBranches;

    private LocalDate applicationDeadline;

    private LocalDate driveDate;

    private Qualification minQualification = Qualification.EITHER;

    private boolean freshersOnly = false;
}
