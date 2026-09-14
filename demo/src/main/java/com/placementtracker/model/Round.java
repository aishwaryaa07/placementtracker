package com.placementtracker.model;

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

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private String name;

    private LocalDate roundDate;

    // Shown to students on the drive/application pages - e.g. what the round covers and
    // what score is needed to pass.
    @Column(length = 2000)
    private String description;

    // Null means this round is graded manually by admin only (no student self-score-submit
    // path). Non-null enables POST /api/student/round-results/{id}/score, which
    // auto-evaluates PASSED/FAILED by comparing the submitted score against this. For
    // THRESHOLD_THEN_TOP_N rounds, this doubles as the minimum-score floor applied before
    // ranking.
    private BigDecimal minScore;

    // Nullable, not defaulted at the DB level (an ALTER TABLE adding a NOT NULL column to a
    // populated table without a DEFAULT fails on Postgres, and this project has no migration
    // tool). null is treated as THRESHOLD everywhere via getEffectiveSelectionMode(), so
    // every pre-existing round keeps its exact current behavior with no backfill.
    @Enumerated(EnumType.STRING)
    private SelectionMode selectionMode;

    // Required (validated in RoundService) when selectionMode is TOP_N or
    // THRESHOLD_THEN_TOP_N - the number of top scorers who pass.
    private Integer topN;

    // Null = not yet finalized (ranking-based rounds only; irrelevant for THRESHOLD rounds,
    // which resolve PASSED/FAILED instantly per submission and never need finalizing).
    private LocalDateTime finalizedAt;

    public SelectionMode getEffectiveSelectionMode() {
        return selectionMode != null ? selectionMode : SelectionMode.THRESHOLD;
    }
}
