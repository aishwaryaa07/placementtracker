package com.placementtracker.dto;

import com.placementtracker.model.Drive;
import lombok.Getter;

@Getter
public class StudentDriveResponse {

    private final DriveResponse drive;
    private final boolean eligible;

    public StudentDriveResponse(Drive drive, boolean eligible) {
        this.drive = new DriveResponse(drive);
        this.eligible = eligible;
    }
}
