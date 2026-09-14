package com.placementtracker.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RoundScoreImportRowPreview {

    private final Long roundResultId;
    private final String studentName;
    private final String studentEmail;
    private final BigDecimal existingScore;
    private final String existingStatus;
    private final BigDecimal newScore;
    private final String predictedStatus;
    private final String rowError;

    public RoundScoreImportRowPreview(Long roundResultId, String studentName, String studentEmail,
            BigDecimal existingScore, String existingStatus, BigDecimal newScore, String predictedStatus, String rowError) {
        this.roundResultId = roundResultId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.existingScore = existingScore;
        this.existingStatus = existingStatus;
        this.newScore = newScore;
        this.predictedStatus = predictedStatus;
        this.rowError = rowError;
    }
}
