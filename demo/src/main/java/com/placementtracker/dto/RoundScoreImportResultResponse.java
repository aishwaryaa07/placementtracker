package com.placementtracker.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class RoundScoreImportResultResponse {

    private final int committedCount;
    private final int skippedCount;
    private final List<RoundScoreImportSkippedRow> skipped;

    public RoundScoreImportResultResponse(int committedCount, List<RoundScoreImportSkippedRow> skipped) {
        this.committedCount = committedCount;
        this.skipped = skipped;
        this.skippedCount = skipped.size();
    }
}
