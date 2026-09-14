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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Links one interviewer to one RoundResult they're allowed to grade. Keyed off the existing
// RoundResult (not a fresh student/round pair) - RoundResult already gets backfilled/self-
// healed by RoundService/ApplicationService, so an assignment always points at a row that's
// guaranteed to exist. Uniqueness is per (interviewer, roundResult), not roundResult alone -
// deliberately allowing a second interviewer to be paired onto the same student/round.
@Entity
@Table(name = "panel_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"interviewer_id", "round_result_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PanelAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_result_id", nullable = false)
    private RoundResult roundResult;

    @Column(nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();
}
