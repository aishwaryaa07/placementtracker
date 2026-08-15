package com.placementtracker.dto;

import com.placementtracker.model.Application;
import com.placementtracker.model.ApplicationStatus;
import com.placementtracker.model.Offer;
import com.placementtracker.model.RoundResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ApplicationResponse {

    private final Long id;
    private final DriveResponse drive;
    private final LocalDateTime appliedAt;
    private final ApplicationStatus status;
    private final List<RoundResultResponse> roundResults;
    private final OfferResponse offer;

    public ApplicationResponse(Application application, List<RoundResult> roundResults, Offer offer) {
        this.id = application.getId();
        this.drive = new DriveResponse(application.getDrive());
        this.appliedAt = application.getAppliedAt();
        this.status = application.getStatus();
        this.roundResults = roundResults.stream().map(RoundResultResponse::new).toList();
        this.offer = offer != null ? new OfferResponse(offer) : null;
    }
}
