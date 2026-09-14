package com.placementtracker.dto;

import com.placementtracker.model.Company;
import lombok.Getter;

@Getter
public class CompanyResponse {

    private final Long id;
    private final String name;
    private final String website;
    private final String description;
    private final String contactEmail;
    private final String logoUrl;
    private final boolean trustedPartner;

    public CompanyResponse(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.website = company.getWebsite();
        this.description = company.getDescription();
        this.contactEmail = company.getContactEmail();
        this.logoUrl = company.getLogoUrl();
        this.trustedPartner = company.isTrustedPartner();
    }
}
