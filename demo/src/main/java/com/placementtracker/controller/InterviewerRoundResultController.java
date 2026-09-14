package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.InterviewerScoreSubmitRequest;
import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.service.InterviewerScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterviewerRoundResultController {

    private final InterviewerScoreService interviewerScoreService;

    @PatchMapping("/api/interviewer/round-results/{id}/score")
    public RoundResultResponse submitScore(
            @AuthenticationPrincipal User interviewer, @PathVariable Long id, @Valid @RequestBody InterviewerScoreSubmitRequest request) {
        return interviewerScoreService.submitScore(interviewer, id, request);
    }
}
