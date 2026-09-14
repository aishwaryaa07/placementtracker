package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.DriveResponse;
import com.placementtracker.dto.InterviewerDriveRequest;
import com.placementtracker.service.DriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// An Interviewer drafts/manages Drive postings for the one company they're linked to. Company
// is always resolved server-side from the authenticated principal - never accepted from the
// client - so an interviewer can never post under a company they don't represent.
@RestController
@RequestMapping("/api/interviewer/drives")
@RequiredArgsConstructor
public class InterviewerDriveController {

    private final DriveService driveService;

    @PostMapping
    public ResponseEntity<DriveResponse> create(
            @Valid @RequestBody InterviewerDriveRequest request, @AuthenticationPrincipal User interviewer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driveService.createForInterviewer(interviewer, request));
    }

    @GetMapping
    public List<DriveResponse> findAll(@AuthenticationPrincipal User interviewer) {
        return driveService.findForInterviewer(interviewer);
    }

    @GetMapping("/{id}")
    public DriveResponse findById(@PathVariable Long id, @AuthenticationPrincipal User interviewer) {
        return driveService.findByIdForInterviewer(interviewer, id);
    }

    @PutMapping("/{id}")
    public DriveResponse update(
            @PathVariable Long id, @Valid @RequestBody InterviewerDriveRequest request,
            @AuthenticationPrincipal User interviewer) {
        return driveService.updateForInterviewer(id, request, interviewer);
    }

    @PatchMapping("/{id}/submit")
    public DriveResponse submit(@PathVariable Long id, @AuthenticationPrincipal User interviewer) {
        return driveService.submitForApproval(id, interviewer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User interviewer) {
        driveService.deleteForInterviewer(id, interviewer);
        return ResponseEntity.noContent().build();
    }
}
