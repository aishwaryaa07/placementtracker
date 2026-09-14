package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.dto.RoundScoreSubmitRequest;
import com.placementtracker.service.RoundResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentRoundResultController {

    private final RoundResultService roundResultService;

    @PatchMapping("/api/student/round-results/{id}/score")
    public RoundResultResponse submitScore(
            @AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody RoundScoreSubmitRequest request) {
        return roundResultService.submitScore(user, id, request);
    }
}
