package com.placementtracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OfferRequest {

    @NotNull
    @DecimalMin(value = "0", message = "CTC offered cannot be negative")
    private BigDecimal ctcOffered;

    private LocalDate offerDate;
}
