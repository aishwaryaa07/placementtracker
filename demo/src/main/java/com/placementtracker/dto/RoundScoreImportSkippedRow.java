package com.placementtracker.dto;

import lombok.Getter;

@Getter
public class RoundScoreImportSkippedRow {

    private final Long roundResultId;
    private final String reason;

    public RoundScoreImportSkippedRow(Long roundResultId, String reason) {
        this.roundResultId = roundResultId;
        this.reason = reason;
    }
}
