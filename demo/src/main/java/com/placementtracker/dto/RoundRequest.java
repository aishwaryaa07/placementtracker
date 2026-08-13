package com.placementtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RoundRequest {

    @NotNull
    private Integer sequence;

    @NotBlank
    private String name;

    private LocalDate roundDate;
}
