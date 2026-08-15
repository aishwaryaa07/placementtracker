package com.placementtracker.controller;

import com.placementtracker.dto.ApplicationResponse;
import com.placementtracker.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/api/admin/drives/{driveId}/applications")
    public List<ApplicationResponse> findByDrive(@PathVariable Long driveId) {
        return applicationService.findByDrive(driveId);
    }

    @GetMapping("/api/admin/applications/{id}")
    public ApplicationResponse findById(@PathVariable Long id) {
        return applicationService.findById(id);
    }
}
