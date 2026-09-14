package com.placementtracker.dto;

import com.placementtracker.model.Drive;
import lombok.Getter;

import java.util.List;

@Getter
public class StudentDriveResponse {

    private final DriveResponse drive;
    private final boolean eligible;
    private final List<EligibilityReasonResponse> ineligibilityReasons;

    public StudentDriveResponse(Drive drive, boolean eligible, List<EligibilityReasonResponse> ineligibilityReasons) {
        this.drive = new DriveResponse(drive);
        this.eligible = eligible;
        this.ineligibilityReasons = ineligibilityReasons;
    }
}
