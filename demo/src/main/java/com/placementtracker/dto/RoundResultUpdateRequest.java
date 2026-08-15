package com.placementtracker.dto;

import com.placementtracker.model.RoundResultStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoundResultUpdateRequest {

    @NotNull
    private RoundResultStatus status;

    private String remarks;
}
