package com.placementtracker.controller;

import com.placementtracker.dto.PanelAssignmentRequest;
import com.placementtracker.dto.PanelAssignmentResponse;
import com.placementtracker.dto.RoundCandidateResponse;
import com.placementtracker.service.PanelAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminPanelAssignmentController {

    private final PanelAssignmentService panelAssignmentService;

    @GetMapping("/api/admin/rounds/{roundId}/panel-assignments")
    public List<PanelAssignmentResponse> findForRound(@PathVariable Long roundId) {
        return panelAssignmentService.findForRound(roundId);
    }

    @GetMapping("/api/admin/rounds/{roundId}/candidates")
    public List<RoundCandidateResponse> findCandidatePool(@PathVariable Long roundId) {
        return panelAssignmentService.findCandidatePool(roundId);
    }

    @PostMapping("/api/admin/rounds/{roundId}/panel-assignments")
    public List<PanelAssignmentResponse> assign(@PathVariable Long roundId, @Valid @RequestBody PanelAssignmentRequest request) {
        return panelAssignmentService.assign(roundId, request);
    }

    @DeleteMapping("/api/admin/panel-assignments/{id}")
    public ResponseEntity<Void> unassign(@PathVariable Long id) {
        panelAssignmentService.unassign(id);
        return ResponseEntity.noContent().build();
    }
}
