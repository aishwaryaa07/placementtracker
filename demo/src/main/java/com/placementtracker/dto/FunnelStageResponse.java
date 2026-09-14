package com.placementtracker.dto;

import lombok.Getter;

@Getter
public class FunnelStageResponse {

    private final Long roundId;
    private final Integer sequence;
    private final String roundName;
    private final int reachedCount;
    private final int passedCount;
    private final int failedCount;
    private final int awaitingCount;

    public FunnelStageResponse(Long roundId, Integer sequence, String roundName, int reachedCount, int passedCount, int failedCount, int awaitingCount) {
        this.roundId = roundId;
        this.sequence = sequence;
        this.roundName = roundName;
        this.reachedCount = reachedCount;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.awaitingCount = awaitingCount;
    }
}
