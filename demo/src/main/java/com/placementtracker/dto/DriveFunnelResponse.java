package com.placementtracker.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class DriveFunnelResponse {

    private final Long driveId;
    private final int totalApplications;
    private final List<FunnelStageResponse> stages;
    private final long offeredCount;

    public DriveFunnelResponse(Long driveId, int totalApplications, List<FunnelStageResponse> stages, long offeredCount) {
        this.driveId = driveId;
        this.totalApplications = totalApplications;
        this.stages = stages;
        this.offeredCount = offeredCount;
    }
}
