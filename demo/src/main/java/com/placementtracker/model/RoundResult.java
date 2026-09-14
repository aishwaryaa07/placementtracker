package com.placementtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;

@Entity
@Table(name = "round_results", uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "round_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RoundResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundResultStatus status = RoundResultStatus.PENDING;

    @Column(length = 1000)
    private String remarks;

    // The student's self-submitted score (via POST /api/student/round-results/{id}/score),
    // for rounds where Round.minScore is set. Null if no score has been submitted, or the
    // round is graded manually by admin instead.
    private BigDecimal score;
}
