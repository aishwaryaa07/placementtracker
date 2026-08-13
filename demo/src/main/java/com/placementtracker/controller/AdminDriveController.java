package com.placementtracker.controller;

import com.placementtracker.dto.DriveRequest;
import com.placementtracker.dto.DriveResponse;
import com.placementtracker.dto.DriveStatusUpdateRequest;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.service.DriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<DriveResponse> create(@Valid @RequestBody DriveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driveService.create(request));
    }

    @GetMapping
    public List<DriveResponse> findAll(
            @RequestParam(required = false) DriveStatus status,
            @RequestParam(required = false) Long companyId) {
        return driveService.findAll(status, companyId);
    }

    @GetMapping("/{id}")
    public DriveResponse findById(@PathVariable Long id) {
        return driveService.findById(id);
    }

    @PutMapping("/{id}")
    public DriveResponse update(@PathVariable Long id, @Valid @RequestBody DriveRequest request) {
        return driveService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public DriveResponse updateStatus(@PathVariable Long id, @Valid @RequestBody DriveStatusUpdateRequest request) {
        return driveService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        driveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
