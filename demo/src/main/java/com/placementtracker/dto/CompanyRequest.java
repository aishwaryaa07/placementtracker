package com.placementtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRequest {

    @NotBlank
    private String name;

    private String website;

    private String description;

    @Email
    private String contactEmail;
}
