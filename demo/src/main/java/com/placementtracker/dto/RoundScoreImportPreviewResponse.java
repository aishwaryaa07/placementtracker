package com.placementtracker.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class RoundScoreImportPreviewResponse {

    private final Long roundId;
    private final int totalDataRows;
    private final int validCount;
    private final int errorCount;
    private final List<RoundScoreImportRowPreview> rows;

    public RoundScoreImportPreviewResponse(Long roundId, List<RoundScoreImportRowPreview> rows) {
        this.roundId = roundId;
        this.rows = rows;
        this.totalDataRows = rows.size();
        this.validCount = (int) rows.stream().filter(r -> r.getRowError() == null).count();
        this.errorCount = totalDataRows - validCount;
    }
}
