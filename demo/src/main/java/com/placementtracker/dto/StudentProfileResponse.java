package com.placementtracker.dto;

import com.placementtracker.model.StudentProfile;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StudentProfileResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String branch;
    private final Integer graduationYear;
    private final BigDecimal cgpa;
    private final String phone;
    private final String resumeUrl;

    public StudentProfileResponse(StudentProfile profile) {
        this.id = profile.getId();
        this.name = profile.getUser().getName();
        this.email = profile.getUser().getEmail();
        this.branch = profile.getBranch();
        this.graduationYear = profile.getGraduationYear();
        this.cgpa = profile.getCgpa();
        this.phone = profile.getPhone();
        this.resumeUrl = profile.getResumeUrl();
    }
}
