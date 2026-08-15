package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.ApplicationResponse;
import com.placementtracker.dto.StudentDriveResponse;
import com.placementtracker.service.ApplicationService;
import com.placementtracker.service.DriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/drives")
@RequiredArgsConstructor
public class StudentDriveController {

    private final DriveService driveService;
    private final ApplicationService applicationService;

    @GetMapping
    public List<StudentDriveResponse> findOpenDrives(@AuthenticationPrincipal User user) {
        return driveService.findOpenDrivesForStudent(user);
    }

    @GetMapping("/{id}")
    public StudentDriveResponse findById(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return driveService.findDriveForStudent(user, id);
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplicationResponse> apply(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(user, id));
    }
}
