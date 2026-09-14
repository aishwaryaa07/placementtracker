package com.placementtracker.dto;

import com.placementtracker.model.SelectionMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RoundRequest {

    @NotNull
    private Integer sequence;

    @NotBlank
    private String name;

    private LocalDate roundDate;

    private String description;

    // Leave null for a round that admin grades manually (PASSED/FAILED). Set it to let
    // students submit their own score for auto PASSED/FAILED evaluation. For
    // THRESHOLD_THEN_TOP_N, this doubles as the minimum-score floor applied before ranking.
    @DecimalMin(value = "0", message = "Minimum score cannot be negative")
    private BigDecimal minScore;

    // Null/omitted defaults to THRESHOLD (see RoundService for cross-field validation:
    // TOP_N/THRESHOLD_THEN_TOP_N require topN; THRESHOLD_THEN_TOP_N also requires minScore).
    private SelectionMode selectionMode;

    @Min(value = 1, message = "Top N must be at least 1")
    private Integer topN;
}
