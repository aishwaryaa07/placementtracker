package com.placementtracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OfferRequest {

    @NotNull
    private BigDecimal ctcOffered;

    private LocalDate offerDate;
}
