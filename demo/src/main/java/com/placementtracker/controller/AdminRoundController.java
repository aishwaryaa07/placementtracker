package com.placementtracker.controller;

import com.placementtracker.dto.RoundRequest;
import com.placementtracker.dto.RoundResponse;
import com.placementtracker.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminRoundController {

    private final RoundService roundService;

    @PostMapping("/api/admin/drives/{driveId}/rounds")
    public ResponseEntity<RoundResponse> create(@PathVariable Long driveId, @Valid @RequestBody RoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundService.create(driveId, request));
    }

    @GetMapping("/api/admin/drives/{driveId}/rounds")
    public List<RoundResponse> findByDrive(@PathVariable Long driveId) {
        return roundService.findByDrive(driveId);
    }

    @PutMapping("/api/admin/rounds/{roundId}")
    public RoundResponse update(@PathVariable Long roundId, @Valid @RequestBody RoundRequest request) {
        return roundService.update(roundId, request);
    }

    @DeleteMapping("/api/admin/rounds/{roundId}")
    public ResponseEntity<Void> delete(@PathVariable Long roundId) {
        roundService.delete(roundId);
        return ResponseEntity.noContent().build();
    }
}
