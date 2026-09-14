package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.DriveApprovalUpdateRequest;
import com.placementtracker.dto.DriveFunnelResponse;
import com.placementtracker.dto.DriveRequest;
import com.placementtracker.dto.DriveResponse;
import com.placementtracker.dto.DriveRevisionResponse;
import com.placementtracker.dto.DriveStatusUpdateRequest;
import com.placementtracker.model.DriveApprovalStatus;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.service.DriveFunnelService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drives")
@RequiredArgsConstructor
public class AdminDriveController {

    private final DriveService driveService;
    private final DriveFunnelService driveFunnelService;

    @PostMapping
    public ResponseEntity<DriveResponse> create(
            @Valid @RequestBody DriveRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driveService.create(request, admin));
    }

    @GetMapping
    public List<DriveResponse> findAll(
            @RequestParam(required = false) DriveStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) DriveApprovalStatus approvalStatus) {
        return driveService.findAll(status, companyId, approvalStatus);
    }

    @GetMapping("/{id}")
    public DriveResponse findById(@PathVariable Long id) {
        return driveService.findById(id);
    }

    @GetMapping("/{id}/funnel")
    public DriveFunnelResponse funnel(@PathVariable Long id) {
        return driveFunnelService.getFunnel(id);
    }

    @GetMapping("/{id}/revisions")
    public List<DriveRevisionResponse> revisions(@PathVariable Long id) {
        return driveService.findRevisions(id);
    }

    @PutMapping("/{id}")
    public DriveResponse update(
            @PathVariable Long id, @Valid @RequestBody DriveRequest request, @AuthenticationPrincipal User admin) {
        return driveService.update(id, request, admin);
    }

    @PatchMapping("/{id}/status")
    public DriveResponse updateStatus(@PathVariable Long id, @Valid @RequestBody DriveStatusUpdateRequest request) {
        return driveService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/approval")
    public DriveResponse updateApproval(
            @PathVariable Long id, @Valid @RequestBody DriveApprovalUpdateRequest request,
            @AuthenticationPrincipal User admin) {
        return driveService.updateApproval(id, request, admin);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        driveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
