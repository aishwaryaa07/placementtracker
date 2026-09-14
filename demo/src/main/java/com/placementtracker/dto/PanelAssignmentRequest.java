package com.placementtracker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PanelAssignmentRequest {

    @NotNull
    private Long interviewerId;

    @NotEmpty
    private List<Long> roundResultIds;
}
