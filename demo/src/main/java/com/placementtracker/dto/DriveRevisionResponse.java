package com.placementtracker.dto;

import com.placementtracker.model.DriveRevision;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DriveRevisionResponse {

    private final Long id;
    private final Long changedById;
    private final String changedByName;
    private final LocalDateTime changedAt;
    private final String fieldName;
    private final String oldValue;
    private final String newValue;

    public DriveRevisionResponse(DriveRevision revision) {
        this.id = revision.getId();
        this.changedById = revision.getChangedBy().getId();
        this.changedByName = revision.getChangedBy().getName();
        this.changedAt = revision.getChangedAt();
        this.fieldName = revision.getFieldName();
        this.oldValue = revision.getOldValue();
        this.newValue = revision.getNewValue();
    }
}
