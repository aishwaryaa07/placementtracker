package com.placementtracker.dto;

import com.placementtracker.User;
import lombok.Getter;

@Getter
public class InterviewerResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final CompanyResponse company;

    public InterviewerResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.company = user.getCompany() != null ? new CompanyResponse(user.getCompany()) : null;
    }
}
