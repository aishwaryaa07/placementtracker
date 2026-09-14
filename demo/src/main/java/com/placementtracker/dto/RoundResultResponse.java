package com.placementtracker.dto;

import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.model.SelectionMode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class RoundResultResponse {

    private final Long id;
    private final Integer roundSequence;
    private final String roundName;
    private final String roundDescription;
    private final LocalDate roundDate;
    private final RoundResultStatus status;
    private final String remarks;
    private final boolean locked;
    private final String lockReason;
    private final BigDecimal score;
    private final BigDecimal minScore;
    private final SelectionMode selectionMode;

    public RoundResultResponse(RoundResult roundResult) {
        this(roundResult, null);
    }

    // lockReason null means unlocked/gradable. Non-null means an earlier round (by sequence)
    // for this same application hasn't been passed yet, so this round isn't reachable - the
    // student can't be graded on it. locked is kept as a plain boolean alongside it so
    // existing `roundResult.locked` checks keep working unchanged.
    public RoundResultResponse(RoundResult roundResult, String lockReason) {
        this.id = roundResult.getId();
        this.roundSequence = roundResult.getRound().getSequence();
        this.roundName = roundResult.getRound().getName();
        this.roundDescription = roundResult.getRound().getDescription();
        this.roundDate = roundResult.getRound().getRoundDate();
        this.status = roundResult.getStatus();
        this.remarks = roundResult.getRemarks();
        this.locked = lockReason != null;
        this.lockReason = lockReason;
        this.score = roundResult.getScore();
        this.minScore = roundResult.getRound().getMinScore();
        this.selectionMode = roundResult.getRound().getEffectiveSelectionMode();
    }
}
