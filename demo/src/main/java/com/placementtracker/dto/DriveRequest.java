package com.placementtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class DriveRequest {

    @NotNull
    private Long companyId;

    @NotBlank
    private String role;

    private String description;

    private BigDecimal ctc;

    private BigDecimal minCgpa;

    private Set<String> eligibleBranches;

    private LocalDate applicationDeadline;

    private LocalDate driveDate;
}
