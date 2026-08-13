package com.placementtracker.dto;

import com.placementtracker.model.Round;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RoundResponse {

    private final Long id;
    private final Integer sequence;
    private final String name;
    private final LocalDate roundDate;

    public RoundResponse(Round round) {
        this.id = round.getId();
        this.sequence = round.getSequence();
        this.name = round.getName();
        this.roundDate = round.getRoundDate();
    }
}
