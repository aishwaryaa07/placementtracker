package com.placementtracker.model;

import com.placementtracker.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "drives")
@Getter
@Setter
@NoArgsConstructor
public class Drive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String role;

    @Column(length = 4000)
    private String description;

    private BigDecimal ctc;

    private BigDecimal minCgpa;

    @ElementCollection
    @CollectionTable(name = "drive_eligible_branches", joinColumns = @JoinColumn(name = "drive_id"))
    @Column(name = "branch")
    private Set<String> eligibleBranches = new HashSet<>();

    private LocalDate applicationDeadline;

    private LocalDate driveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriveStatus status = DriveStatus.UPCOMING;

    // Who authored this posting - an Admin (direct authorship) or an Interviewer drafting on
    // behalf of their linked company. Null only for drives created before this field existed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // columnDefinition (not just nullable=false) is deliberate: this table already has real
    // rows in every environment this app runs in (no Flyway/Liquibase, just ddl-auto=update),
    // and a plain NOT NULL ADD COLUMN fails against a populated Postgres table with no
    // default. The DB-level default backfills existing rows to APPROVED - the correct read
    // of "this drive was already live before the approval workflow existed" - while new
    // entities still get DRAFT from the Java-side field initializer below (an explicit value
    // is always sent on INSERT, so the DB default only ever fires for the ALTER TABLE itself).
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) not null default 'APPROVED'")
    private DriveApprovalStatus approvalStatus = DriveApprovalStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) not null default 'EITHER'")
    private Qualification minQualification = Qualification.EITHER;

    @Column(columnDefinition = "boolean not null default false")
    private boolean freshersOnly = false;

    @OneToMany(mappedBy = "drive", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<Round> rounds = new ArrayList<>();

    // minQualification/freshersOnly are display-only: there's no qualification/experience
    // field on StudentProfile to compare against, so they deliberately aren't wired into
    // isEligibleFor. They just surface the JD's declared requirement for Admin/Student to
    // read - not a claim about whether any given student meets it.
    public boolean isEligibleFor(StudentProfile student) {
        return branchMatches(student) && cgpaMeetsMinimum(student);
    }

    // Branch codes are free-text (no enum/lookup table), so real data drifts in case and
    // whitespace ("CSE" vs "cse" vs " CSE "). Compared trimmed + case-insensitively so that
    // drift doesn't silently make an otherwise-eligible student show as ineligible.
    public boolean branchMatches(StudentProfile student) {
        if (eligibleBranches == null || eligibleBranches.isEmpty()) {
            return true;
        }
        if (student.getBranch() == null || student.getBranch().isBlank()) {
            return false;
        }
        String studentBranch = student.getBranch().trim();
        return eligibleBranches.stream()
                .anyMatch(branch -> branch != null && branch.trim().equalsIgnoreCase(studentBranch));
    }

    public boolean cgpaMeetsMinimum(StudentProfile student) {
        return minCgpa == null || (student.getCgpa() != null && student.getCgpa().compareTo(minCgpa) >= 0);
    }
}
