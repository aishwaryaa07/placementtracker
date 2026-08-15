package com.placementtracker.controller;

import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.dto.RoundResultUpdateRequest;
import com.placementtracker.service.RoundResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminRoundResultController {

    private final RoundResultService roundResultService;

    @PatchMapping("/api/admin/round-results/{id}")
    public RoundResultResponse update(@PathVariable Long id, @Valid @RequestBody RoundResultUpdateRequest request) {
        return roundResultService.update(id, request);
    }
}
