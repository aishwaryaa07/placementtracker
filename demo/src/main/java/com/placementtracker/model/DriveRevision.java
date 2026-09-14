package com.placementtracker.model;

import com.placementtracker.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// One row per changed field per Drive update - the field-level audit trail for dispute
// resolution ("the company says they never listed 3 LPA"). Written by DriveService.update
// for every field that actually changed, not just the ones that trigger re-approval.
@Entity
@Table(name = "drive_revisions")
@Getter
@Setter
@NoArgsConstructor
public class DriveRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(nullable = false)
    private String fieldName;

    @Column(length = 2000)
    private String oldValue;

    @Column(length = 2000)
    private String newValue;
}
