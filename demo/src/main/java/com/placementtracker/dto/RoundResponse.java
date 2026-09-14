package com.placementtracker.dto;

import com.placementtracker.model.Round;
import com.placementtracker.model.SelectionMode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class RoundResponse {

    private final Long id;
    private final Integer sequence;
    private final String name;
    private final LocalDate roundDate;
    private final String description;
    private final BigDecimal minScore;
    private final SelectionMode selectionMode;
    private final Integer topN;
    private final LocalDateTime finalizedAt;

    public RoundResponse(Round round) {
        this.id = round.getId();
        this.sequence = round.getSequence();
        this.name = round.getName();
        this.roundDate = round.getRoundDate();
        this.description = round.getDescription();
        this.minScore = round.getMinScore();
        this.selectionMode = round.getEffectiveSelectionMode();
        this.topN = round.getTopN();
        this.finalizedAt = round.getFinalizedAt();
    }
}
