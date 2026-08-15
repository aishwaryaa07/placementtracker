package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.StudentProfileRequest;
import com.placementtracker.dto.StudentProfileResponse;
import com.placementtracker.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public StudentProfileResponse getMyProfile(@AuthenticationPrincipal User user) {
        return studentProfileService.getMyProfile(user);
    }

    @PutMapping
    public StudentProfileResponse upsertProfile(
            @AuthenticationPrincipal User user, @Valid @RequestBody StudentProfileRequest request) {
        return studentProfileService.upsertProfile(user, request);
    }
}
