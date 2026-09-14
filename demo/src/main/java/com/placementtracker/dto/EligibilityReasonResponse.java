package com.placementtracker.dto;

import lombok.Getter;

@Getter
public class EligibilityReasonResponse {

    private final String type;
    private final String message;

    public EligibilityReasonResponse(String type, String message) {
        this.type = type;
        this.message = message;
    }
}
