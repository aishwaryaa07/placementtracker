package com.placementtracker.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StudentProfileRequest {
    private String branch;
    private Integer graduationYear;
    private BigDecimal cgpa;
    private String phone;
    private String resumeUrl;
}
