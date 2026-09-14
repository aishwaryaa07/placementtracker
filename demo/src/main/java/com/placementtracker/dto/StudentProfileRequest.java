package com.placementtracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StudentProfileRequest {

    @NotBlank(message = "Branch is required")
    private String branch;

    @NotNull(message = "Graduation year is required")
    @Min(value = 1950, message = "Enter a valid graduation year")
    @Max(value = 2100, message = "Enter a valid graduation year")
    private Integer graduationYear;

    @NotNull(message = "CGPA is required")
    @DecimalMin(value = "1.0", message = "CGPA must be at least 1.0")
    @DecimalMax(value = "10.0", message = "CGPA must be at most 10.0")
    @Digits(integer = 2, fraction = 1, message = "CGPA can have at most one decimal place, e.g. 8.5")
    private BigDecimal cgpa;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+(91|1|44)\\d{10}$",
            message = "Enter a valid phone number with country code (+91, +1 or +44) and exactly 10 digits")
    private String phone;

    @NotBlank(message = "Resume is required")
    @Pattern(regexp = "^https?://.+", message = "Resume URL must start with http:// or https://")
    private String resumeUrl;

    @NotBlank(message = "10th marksheet is required")
    @Pattern(regexp = "^https?://.+", message = "10th marksheet must be an uploaded file")
    private String tenthMarksheetUrl;

    @NotBlank(message = "12th marksheet is required")
    @Pattern(regexp = "^https?://.+", message = "12th marksheet must be an uploaded file")
    private String twelfthMarksheetUrl;

    @NotNull(message = "Most recent semester CGPA is required")
    @DecimalMin(value = "1.0", message = "Most recent semester CGPA must be at least 1.0")
    @DecimalMax(value = "10.0", message = "Most recent semester CGPA must be at most 10.0")
    @Digits(integer = 2, fraction = 1, message = "Most recent semester CGPA can have at most one decimal place, e.g. 8.5")
    private BigDecimal recentSemesterCgpa;
}
