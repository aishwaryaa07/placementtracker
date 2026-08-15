package com.placementtracker.dto;

import com.placementtracker.model.OfferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfferStatusUpdateRequest {

    @NotNull
    private OfferStatus status;
}
