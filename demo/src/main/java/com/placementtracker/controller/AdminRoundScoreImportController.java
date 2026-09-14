package com.placementtracker.controller;

import com.placementtracker.dto.RoundScoreImportConfirmRequest;
import com.placementtracker.dto.RoundScoreImportPreviewResponse;
import com.placementtracker.dto.RoundScoreImportResultResponse;
import com.placementtracker.service.RoundScoreImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class AdminRoundScoreImportController {

    private final RoundScoreImportService roundScoreImportService;

    @GetMapping("/api/admin/rounds/{roundId}/score-template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long roundId) {
        byte[] body = roundScoreImportService.buildTemplate(roundId).getBytes(StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("round-" + roundId + "-scores.csv")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    @PostMapping("/api/admin/rounds/{roundId}/score-import/preview")
    public RoundScoreImportPreviewResponse preview(@PathVariable Long roundId, @RequestParam("file") MultipartFile file) {
        return roundScoreImportService.preview(roundId, file);
    }

    @PostMapping("/api/admin/rounds/{roundId}/score-import/confirm")
    public RoundScoreImportResultResponse confirm(@PathVariable Long roundId, @Valid @RequestBody RoundScoreImportConfirmRequest request) {
        return roundScoreImportService.confirm(roundId, request);
    }
}
