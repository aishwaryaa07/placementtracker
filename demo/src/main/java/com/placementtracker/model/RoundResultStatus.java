package com.placementtracker.model;

public enum RoundResultStatus {
    PENDING,
    // Score recorded for a TOP_N / THRESHOLD_THEN_TOP_N round, but not yet ranked - the
    // round hasn't been finalized. Distinct from PENDING so "no score yet" and "scored,
    // awaiting finalize" aren't conflated; distinct from PASSED so later rounds stay locked.
    SCORED,
    PASSED,
    FAILED
}
