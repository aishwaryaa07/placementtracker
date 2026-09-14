package com.placementtracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoundScoreImportConfirmRequest {

    @NotEmpty
    @Valid
    private List<RoundScoreImportRowRequest> rows;
}
