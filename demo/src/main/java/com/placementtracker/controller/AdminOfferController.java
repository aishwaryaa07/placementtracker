package com.placementtracker.controller;

import com.placementtracker.dto.OfferRequest;
import com.placementtracker.dto.OfferResponse;
import com.placementtracker.dto.OfferStatusUpdateRequest;
import com.placementtracker.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminOfferController {

    private final OfferService offerService;

    @PostMapping("/api/admin/applications/{applicationId}/offer")
    public ResponseEntity<OfferResponse> create(
            @PathVariable Long applicationId, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(applicationId, request));
    }

    @PatchMapping("/api/admin/offers/{id}/status")
    public OfferResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OfferStatusUpdateRequest request) {
        return offerService.updateStatus(id, request);
    }
}
