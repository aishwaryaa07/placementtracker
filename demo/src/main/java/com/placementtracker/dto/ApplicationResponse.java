package com.placementtracker.dto;

import com.placementtracker.model.Application;
import com.placementtracker.model.ApplicationStatus;
import com.placementtracker.model.Offer;
import com.placementtracker.model.RoundResult;
import com.placementtracker.util.RoundResultLockCalculator;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class ApplicationResponse {

    private final Long id;
    private final String studentName;
    private final String studentEmail;
    private final String studentBranch;
    private final DriveResponse drive;
    private final LocalDateTime appliedAt;
    private final ApplicationStatus status;
    private final List<RoundResultResponse> roundResults;
    private final OfferResponse offer;

    public ApplicationResponse(Application application, List<RoundResult> roundResults, Offer offer) {
        this.id = application.getId();
        this.studentName = application.getStudent().getUser().getName();
        this.studentEmail = application.getStudent().getUser().getEmail();
        this.studentBranch = application.getStudent().getBranch();
        this.drive = new DriveResponse(application.getDrive());
        this.appliedAt = application.getAppliedAt();
        this.status = application.getStatus();
        this.roundResults = withLockedFlags(roundResults);
        this.offer = offer != null ? new OfferResponse(offer) : null;
    }

    // The API's own ordering of roundResults (as returned by the repository) is left
    // untouched - only RoundResultLockCalculator's internal computation is sequence-sorted.
    private static List<RoundResultResponse> withLockedFlags(List<RoundResult> roundResults) {
        Map<Long, String> lockReasonByRoundResultId = RoundResultLockCalculator.lockReasons(roundResults);
        return roundResults.stream()
                .map(rr -> new RoundResultResponse(rr, lockReasonByRoundResultId.get(rr.getId())))
                .toList();
    }
}
