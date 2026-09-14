package com.placementtracker.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RoundFinalizeResultResponse {

    private final Long roundId;
    private final long passedCount;
    private final long failedCount;
    private final int totalConsidered;
    private final LocalDateTime finalizedAt;

    public RoundFinalizeResultResponse(Long roundId, long passedCount, long failedCount, int totalConsidered, LocalDateTime finalizedAt) {
        this.roundId = roundId;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.totalConsidered = totalConsidered;
        this.finalizedAt = finalizedAt;
    }
}
