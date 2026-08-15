package com.placementtracker.dto;

import com.placementtracker.model.Offer;
import com.placementtracker.model.OfferStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class OfferResponse {

    private final Long id;
    private final BigDecimal ctcOffered;
    private final LocalDate offerDate;
    private final OfferStatus status;

    public OfferResponse(Offer offer) {
        this.id = offer.getId();
        this.ctcOffered = offer.getCtcOffered();
        this.offerDate = offer.getOfferDate();
        this.status = offer.getStatus();
    }
}
