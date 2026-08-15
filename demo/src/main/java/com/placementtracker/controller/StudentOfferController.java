package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.OfferResponse;
import com.placementtracker.dto.OfferStatusUpdateRequest;
import com.placementtracker.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentOfferController {

    private final OfferService offerService;

    @PatchMapping("/api/student/offers/{id}/status")
    public OfferResponse respond(
            @AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody OfferStatusUpdateRequest request) {
        return offerService.respondToOffer(user, id, request);
    }
}
