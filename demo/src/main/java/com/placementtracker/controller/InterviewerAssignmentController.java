package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.PanelAssignmentResponse;
import com.placementtracker.service.PanelAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InterviewerAssignmentController {

    private final PanelAssignmentService panelAssignmentService;

    @GetMapping("/api/interviewer/assignments")
    public List<PanelAssignmentResponse> myAssignments(
            @AuthenticationPrincipal User interviewer, @RequestParam(required = false) Long roundId) {
        return panelAssignmentService.findMyAssignments(interviewer, roundId);
    }
}
