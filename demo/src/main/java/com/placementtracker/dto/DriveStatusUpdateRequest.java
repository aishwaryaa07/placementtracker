package com.placementtracker.dto;

import com.placementtracker.model.DriveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriveStatusUpdateRequest {

    @NotNull
    private DriveStatus status;
}
