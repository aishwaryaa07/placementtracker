package com.placementtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String website;

    @Column(length = 2000)
    private String description;

    private String contactEmail;

    // A direct image URL (e.g. a favicon-service URL for the company's own official domain)
    // shown in place of the initials-avatar wherever this company's logo appears. Null is
    // the normal case - CompanyLogo falls back to the initials avatar.
    private String logoUrl;

    // Trusted companies' interviewer-submitted drives skip the PENDING_APPROVAL queue and go
    // straight to APPROVED on create/submit. Does not affect re-approval on sensitive edits -
    // those always re-enter the queue regardless of this flag.
    //
    // columnDefinition (not just nullable=false): this table already has real rows in every
    // environment this app runs in (ddl-auto=update, no Flyway/Liquibase) - a plain NOT NULL
    // ADD COLUMN fails against a populated Postgres table with no default, exactly like the
    // same fix already applied to Drive's new enum/boolean fields.
    @Column(columnDefinition = "boolean not null default false")
    private boolean trustedPartner = false;
}
