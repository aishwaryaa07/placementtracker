package com.placementtracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InterviewerScoreSubmitRequest {

    @NotNull
    @DecimalMin(value = "0", message = "Score cannot be negative")
    private BigDecimal score;

    private String remarks;
}
