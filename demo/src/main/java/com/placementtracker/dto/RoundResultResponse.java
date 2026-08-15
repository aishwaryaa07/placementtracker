package com.placementtracker.dto;

import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import lombok.Getter;

@Getter
public class RoundResultResponse {

    private final Long id;
    private final Integer roundSequence;
    private final String roundName;
    private final RoundResultStatus status;
    private final String remarks;

    public RoundResultResponse(RoundResult roundResult) {
        this.id = roundResult.getId();
        this.roundSequence = roundResult.getRound().getSequence();
        this.roundName = roundResult.getRound().getName();
        this.status = roundResult.getStatus();
        this.remarks = roundResult.getRemarks();
    }
}
