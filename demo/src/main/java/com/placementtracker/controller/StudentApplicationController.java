package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.ApplicationResponse;
import com.placementtracker.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/applications")
@RequiredArgsConstructor
public class StudentApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationResponse> findMyApplications(@AuthenticationPrincipal User user) {
        return applicationService.findMyApplications(user);
    }

    @GetMapping("/{id}")
    public ApplicationResponse findById(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return applicationService.findMyApplicationById(user, id);
    }
}
